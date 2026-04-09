package com.rich.app.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.rich.ai.service.AiCustomerService;
import com.rich.common.constant.CustomerServiceConstant;
import com.rich.common.constant.StreamEventConstant;
import com.rich.app.factory.AiCustomerServiceFactory;
import com.rich.app.model.StreamEvent;
import com.rich.app.model.StreamSession;
import com.rich.app.service.CustomerServiceConversationService;
import com.rich.app.service.CustomerServiceMessageService;
import com.rich.app.service.CustomerServiceService;
import com.rich.app.service.StreamSessionService;
import com.rich.app.utils.ConvertTokenStreamToFluxUtils.ConvertCustomerServiceTokenStreamToFluxUtils;
import com.rich.common.constant.UserConstant;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.model.dto.customerservice.CustomerServiceChatRequest;
import com.rich.model.entity.CustomerServiceConversation;
import com.rich.model.entity.User;
import com.rich.model.enums.CustomerServiceMessageRoleEnum;
import com.rich.model.vo.CustomerServiceConversationVO;
import com.rich.model.vo.CustomerServiceMessageVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI 客服聚合 Service 实现
 *
 * @author DuRuiChi
 * @create 2026/4/9
 */
@Slf4j
@Service
public class CustomerServiceServiceImpl implements CustomerServiceService {

    /**
     * 跟随流轮询间隔
     */
    private static final long FOLLOW_POLL_INTERVAL_MILLIS = 100L;

    /**
     * AI 客服默认错误文案
     */
    private static final String AI_RESPONSE_FAILED_MESSAGE = "AI 客服响应失败";

    /**
     * 未知错误文案
     */
    private static final String UNKNOWN_ERROR_MESSAGE = "未知错误";

    /**
     * 游客流式用户 ID 偏移量
     */
    private static final long GUEST_STREAM_USER_ID_OFFSET = 1L;

    @Resource
    private CustomerServiceConversationService customerServiceConversationService;

    @Resource
    private CustomerServiceMessageService customerServiceMessageService;

    @Resource
    private AiCustomerServiceFactory aiCustomerServiceFactory;

    @Resource
    private ConvertCustomerServiceTokenStreamToFluxUtils convertCustomerServiceTokenStreamToFluxUtils;

    @Resource
    private StreamSessionService streamSessionService;

    /**
     * 创建客服会话
     *
     * @param request 请求对象
     * @return 会话视图对象
     */
    @Override
    public CustomerServiceConversationVO createConversation(HttpServletRequest request) {
        VisitorContext visitorContext = getVisitorContext(request);
        CustomerServiceConversation conversation = customerServiceConversationService
                .createConversation(visitorContext.userId(), visitorContext.visitorKey());
        return toConversationVO(conversation);
    }

    /**
     * 查询当前访客可见的会话列表
     *
     * @param request 请求对象
     * @return 会话列表
     */
    @Override
    public List<CustomerServiceConversationVO> listConversations(HttpServletRequest request) {
        VisitorContext visitorContext = getVisitorContext(request);
        return customerServiceConversationService.listUserConversations(visitorContext.userId(), visitorContext.visitorKey());
    }

    /**
     * 查询指定会话的消息列表
     *
     * @param conversationId 会话 ID
     * @param pageSize 分页大小
     * @param lastCreateTime 上一页最后一条时间
     * @param request 请求对象
     * @return 消息列表
     */
    @Override
    public List<CustomerServiceMessageVO> listMessages(Long conversationId, int pageSize, LocalDateTime lastCreateTime,
                                                       HttpServletRequest request) {
        VisitorContext visitorContext = getVisitorContext(request);
        customerServiceConversationService.verifyAndGetConversation(conversationId, visitorContext.userId(), visitorContext.visitorKey());
        return customerServiceMessageService.listConversationMessages(conversationId, pageSize, lastCreateTime);
    }

    /**
     * 发起或续接 AI 客服流式会话
     *
     * @param chatRequest 对话请求
     * @param request 请求对象
     * @return SSE 事件流
     */
    @Override
    public Flux<ServerSentEvent<String>> chatStream(CustomerServiceChatRequest chatRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(chatRequest.getConversationId() == null || chatRequest.getConversationId() <= 0,
                ErrorCode.PARAMS_ERROR, "会话ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(chatRequest.getMessage()), ErrorCode.PARAMS_ERROR, "消息不能为空");
        VisitorContext visitorContext = getVisitorContext(request);
        CustomerServiceConversation conversation = customerServiceConversationService.verifyAndGetConversation(
                chatRequest.getConversationId(), visitorContext.userId(), visitorContext.visitorKey());
        String sessionKey = generateSessionKey(conversation.getId(), visitorContext.identityKey(), chatRequest.getMessage());
        boolean reconnect = Boolean.TRUE.equals(chatRequest.getReconnect());
        StreamSession existingSession = streamSessionService.getSession(sessionKey);

        if (reconnect) {
            if (existingSession != null) {
                return createFollowFlux(sessionKey, chatRequest.getLastEventId());
            }
            return buildEndEventFlux();
        }

        if (hasActiveSession(existingSession)) {
            return createFollowFlux(sessionKey, chatRequest.getLastEventId());
        }

        // 先落一条用户消息，再开始 AI 回答
        customerServiceMessageService.addMessage(conversation.getId(), CustomerServiceMessageRoleEnum.USER.getValue(),
                chatRequest.getMessage(), visitorContext.userId(), visitorContext.visitorKey());
        customerServiceConversationService.refreshConversation(conversation.getId(), chatRequest.getMessage(),
                LocalDateTime.now(), true);
        streamSessionService.createSession(sessionKey, conversation.getId(), visitorContext.streamUserId());
        startAiResponseStreaming(conversation.getId(), sessionKey, chatRequest.getMessage(), visitorContext);
        return createFollowFlux(sessionKey, chatRequest.getLastEventId());
    }

    /**
     * 启动 AI 客服响应流
     *
     * @param conversationId 会话 ID
     * @param sessionKey 流式会话 key
     * @param userMessage 用户消息
     * @param visitorContext 访客上下文
     */
    private void startAiResponseStreaming(Long conversationId, String sessionKey, String userMessage, VisitorContext visitorContext) {
        log.info("【AI 客服】开始调用 AI，conversationId={}，CUSTOMER_SERVICE RAG 启用状态={}",
                conversationId, aiCustomerServiceFactory.isCustomerServiceRagEnabled());
        AiCustomerService customerService = aiCustomerServiceFactory.getCustomerService(conversationId);
        StringBuilder aiResponseBuilder = new StringBuilder();
        Flux<String> responseFlux = convertCustomerServiceTokenStreamToFluxUtils.convertTokenStreamToFlux(
                customerService.chatStream(userMessage, conversationId));
        responseFlux.subscribe(chunk -> {
                    if (StrUtil.isEmpty(chunk)) {
                        return;
                    }
                    // 实时缓存 AI 输出，并把增量片段推送给前端
                    aiResponseBuilder.append(chunk);
                    addMessageEvent(sessionKey, chunk);
                }, error -> {
                    log.error("AI 客服流式响应失败，conversationId={}", conversationId, error);
                    String errorMsg = error.getMessage() != null ? error.getMessage() : AI_RESPONSE_FAILED_MESSAGE;
                    addServerErrorEvent(sessionKey, errorMsg);
                    streamSessionService.markError(sessionKey, errorMsg);
                }, () -> {
                    String aiResponse = aiResponseBuilder.toString();
                    if (StrUtil.isNotBlank(aiResponse)) {
                        customerServiceMessageService.addMessage(conversationId, CustomerServiceMessageRoleEnum.AI.getValue(),
                                aiResponse, visitorContext.userId(), visitorContext.visitorKey());
                        customerServiceConversationService.refreshConversation(conversationId, aiResponse,
                                LocalDateTime.now(), false);
                    }
                    addEndEvent(sessionKey);
                    streamSessionService.markCompleted(sessionKey);
                });
    }

    /**
     * 跟随既有流式会话，持续向前端推送增量事件
     *
     * @param sessionKey 流式会话 key
     * @param lastEventId 前端最后收到的事件 ID
     * @return SSE 事件流
     */
    private Flux<ServerSentEvent<String>> createFollowFlux(String sessionKey, String lastEventId) {
        AtomicReference<String> lastSentEventId = new AtomicReference<>(lastEventId);
        return Flux.interval(Duration.ofMillis(FOLLOW_POLL_INTERVAL_MILLIS))
                .flatMap(tick -> {
                    streamSessionService.updateLastAccessTime(sessionKey);
                    StreamSession session = streamSessionService.getSession(sessionKey);
                    if (session == null) {
                        return buildEndEventFlux();
                    }
                    List<StreamEvent> newEvents = streamSessionService.getEventsAfter(sessionKey, lastSentEventId.get());
                    if (!newEvents.isEmpty()) {
                        StreamEvent lastEvent = newEvents.get(newEvents.size() - 1);
                        lastSentEventId.set(lastEvent.getEventId());
                        return Flux.fromIterable(newEvents)
                                .map(event -> ServerSentEvent.<String>builder()
                                        .id(event.getEventId())
                                        .event(event.getEventType())
                                        .data(event.getData())
                                        .build());
                    }
                    if (session.isCompleted()) {
                        return buildEndEventFlux();
                    }
                    if (session.isHasError()) {
                        String errorMsg = session.getErrorMessage() != null ? session.getErrorMessage() : UNKNOWN_ERROR_MESSAGE;
                        return Flux.just(buildServerErrorSseEvent(errorMsg));
                    }
                    return Flux.empty();
                })
                .takeUntil(event -> StreamEventConstant.EVENT_END.equals(event.event())
                        || StreamEventConstant.EVENT_SERVER_ERROR.equals(event.event()));
    }

    /**
     * 生成 AI 客服流式会话 key
     *
     * @param conversationId 会话 ID
     * @param identityKey 身份标识
     * @param message 用户消息
     * @return 流式会话 key
     */
    private String generateSessionKey(Long conversationId, String identityKey, String message) {
        ThrowUtils.throwIf(conversationId == null || conversationId <= 0 || StrUtil.isBlank(identityKey) || StrUtil.isBlank(message),
                ErrorCode.PARAMS_ERROR, "生成 AI 客服会话密钥失败");
        return String.format(CustomerServiceConstant.SESSION_KEY_TEMPLATE, conversationId, (identityKey + message).hashCode());
    }

    /**
     * 获取当前请求对应的访客上下文
     * 已登录用户优先走用户身份，未登录则回退到游客身份
     *
     * @param request 请求对象
     * @return 访客上下文
     */
    private VisitorContext getVisitorContext(HttpServletRequest request) {
        Object loginUser = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (loginUser instanceof User user && user.getId() != null) {
            return new VisitorContext(user.getId(), null, CustomerServiceConstant.USER_IDENTITY_PREFIX + user.getId(), user.getId());
        }
        String guestKey = (String) request.getSession().getAttribute(CustomerServiceConstant.GUEST_VISITOR_SESSION_KEY);
        if (StrUtil.isBlank(guestKey)) {
            guestKey = CustomerServiceConstant.GUEST_IDENTITY_PREFIX + IdUtil.simpleUUID();
            request.getSession().setAttribute(CustomerServiceConstant.GUEST_VISITOR_SESSION_KEY, guestKey);
        }
        long streamUserId = Math.abs((long) guestKey.hashCode()) + GUEST_STREAM_USER_ID_OFFSET;
        return new VisitorContext(null, guestKey, guestKey, streamUserId);
    }

    /**
     * 转换会话实体为视图对象
     *
     * @param conversation 会话实体
     * @return 会话视图对象
     */
    private CustomerServiceConversationVO toConversationVO(CustomerServiceConversation conversation) {
        CustomerServiceConversationVO conversationVO = new CustomerServiceConversationVO();
        conversationVO.setId(conversation.getId());
        conversationVO.setTitle(conversation.getTitle());
        conversationVO.setLastMessagePreview(conversation.getLastMessagePreview());
        conversationVO.setLastMessageTime(conversation.getLastMessageTime());
        conversationVO.setCreateTime(conversation.getCreateTime());
        conversationVO.setUpdateTime(conversation.getUpdateTime());
        return conversationVO;
    }

    /**
     * 判断当前会话是否还在流式处理中
     *
     * @param session 流式会话
     * @return 是否仍可续接
     */
    private boolean hasActiveSession(StreamSession session) {
        return session != null && !session.isCompleted() && !session.isHasError();
    }

    /**
     * 记录普通消息事件
     *
     * @param sessionKey 流式会话 key
     * @param chunk 文本片段
     */
    private void addMessageEvent(String sessionKey, String chunk) {
        String eventId = streamSessionService.generateEventId(sessionKey);
        streamSessionService.addEvent(sessionKey,
                buildStreamEvent(eventId, StreamEventConstant.EVENT_MESSAGE, buildChunkData(chunk)));
    }

    /**
     * 记录服务端错误事件
     *
     * @param sessionKey 流式会话 key
     * @param errorMsg 错误信息
     */
    private void addServerErrorEvent(String sessionKey, String errorMsg) {
        String eventId = streamSessionService.generateEventId(sessionKey);
        streamSessionService.addEvent(sessionKey,
                buildStreamEvent(eventId, StreamEventConstant.EVENT_SERVER_ERROR, buildErrorData(errorMsg)));
    }

    /**
     * 记录结束事件
     *
     * @param sessionKey 流式会话 key
     */
    private void addEndEvent(String sessionKey) {
        String eventId = streamSessionService.generateEventId(sessionKey);
        streamSessionService.addEvent(sessionKey,
                buildStreamEvent(eventId, StreamEventConstant.EVENT_END, StreamEventConstant.EMPTY_DATA));
    }

    /**
     * 构造通用流事件对象
     *
     * @param eventId 事件 ID
     * @param eventType 事件类型
     * @param data 事件数据
     * @return 流事件
     */
    private StreamEvent buildStreamEvent(String eventId, String eventType, String data) {
        return StreamEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .data(data)
                .build();
    }

    /**
     * 构造结束事件流
     *
     * @return 仅包含结束事件的 Flux
     */
    private Flux<ServerSentEvent<String>> buildEndEventFlux() {
        return Flux.just(buildEndSseEvent());
    }

    /**
     * 构造结束 SSE 事件
     *
     * @return 结束 SSE 事件
     */
    private ServerSentEvent<String> buildEndSseEvent() {
        return ServerSentEvent.<String>builder()
                .event(StreamEventConstant.EVENT_END)
                .data(StreamEventConstant.EMPTY_DATA)
                .build();
    }

    /**
     * 构造错误 SSE 事件
     *
     * @param errorMsg 错误信息
     * @return 错误 SSE 事件
     */
    private ServerSentEvent<String> buildServerErrorSseEvent(String errorMsg) {
        return ServerSentEvent.<String>builder()
                .event(StreamEventConstant.EVENT_SERVER_ERROR)
                .data(buildErrorData(errorMsg))
                .build();
    }

    /**
     * 构造消息片段 JSON 数据
     *
     * @param chunk 文本片段
     * @return JSON 数据
     */
    private String buildChunkData(String chunk) {
        return JSONUtil.toJsonStr(Map.of(StreamEventConstant.DATA_BLOCK_KEY, chunk));
    }

    /**
     * 构造错误 JSON 数据
     *
     * @param errorMsg 错误信息
     * @return JSON 数据
     */
    private String buildErrorData(String errorMsg) {
        return JSONUtil.toJsonStr(Map.of(StreamEventConstant.DATA_ERROR_KEY, errorMsg));
    }

    /**
     * 访客上下文
     * 用于统一封装登录用户和游客身份信息
     */
    private record VisitorContext(Long userId, String visitorKey, String identityKey, Long streamUserId) {
    }
}
