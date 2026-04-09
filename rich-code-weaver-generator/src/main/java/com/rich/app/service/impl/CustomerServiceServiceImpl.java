package com.rich.app.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.rich.ai.service.AiCustomerService;
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

    private static final String GUEST_VISITOR_KEY = "customer_service_guest_key";

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

    @Override
    public CustomerServiceConversationVO createConversation(HttpServletRequest request) {
        VisitorContext visitorContext = getVisitorContext(request);
        CustomerServiceConversation conversation = customerServiceConversationService
                .createConversation(visitorContext.userId(), visitorContext.visitorKey());
        CustomerServiceConversationVO conversationVO = new CustomerServiceConversationVO();
        conversationVO.setId(conversation.getId());
        conversationVO.setTitle(conversation.getTitle());
        conversationVO.setLastMessagePreview(conversation.getLastMessagePreview());
        conversationVO.setLastMessageTime(conversation.getLastMessageTime());
        conversationVO.setCreateTime(conversation.getCreateTime());
        conversationVO.setUpdateTime(conversation.getUpdateTime());
        return conversationVO;
    }

    @Override
    public List<CustomerServiceConversationVO> listConversations(HttpServletRequest request) {
        VisitorContext visitorContext = getVisitorContext(request);
        return customerServiceConversationService.listUserConversations(visitorContext.userId(), visitorContext.visitorKey());
    }

    @Override
    public List<CustomerServiceMessageVO> listMessages(Long conversationId, int pageSize, LocalDateTime lastCreateTime,
                                                       HttpServletRequest request) {
        VisitorContext visitorContext = getVisitorContext(request);
        customerServiceConversationService.verifyAndGetConversation(conversationId, visitorContext.userId(), visitorContext.visitorKey());
        return customerServiceMessageService.listConversationMessages(conversationId, pageSize, lastCreateTime);
    }

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
            return Flux.just(ServerSentEvent.<String>builder().event("end").data("").build());
        }

        if (existingSession != null && !existingSession.isCompleted() && !existingSession.isHasError()) {
            return createFollowFlux(sessionKey, chatRequest.getLastEventId());
        }

        customerServiceMessageService.addMessage(conversation.getId(), CustomerServiceMessageRoleEnum.USER.getValue(),
                chatRequest.getMessage(), visitorContext.userId(), visitorContext.visitorKey());
        customerServiceConversationService.refreshConversation(conversation.getId(), chatRequest.getMessage(),
                LocalDateTime.now(), true);
        streamSessionService.createSession(sessionKey, conversation.getId(), visitorContext.streamUserId());
        startAiResponseStreaming(conversation.getId(), sessionKey, chatRequest.getMessage(), visitorContext);
        return createFollowFlux(sessionKey, chatRequest.getLastEventId());
    }

    private void startAiResponseStreaming(Long conversationId, String sessionKey, String userMessage, VisitorContext visitorContext) {
        AiCustomerService customerService = aiCustomerServiceFactory.getCustomerService(conversationId);
        StringBuilder aiResponseBuilder = new StringBuilder();
        Flux<String> responseFlux = convertCustomerServiceTokenStreamToFluxUtils.convertTokenStreamToFlux(
                customerService.chatStream(userMessage, conversationId));
        responseFlux.subscribe(chunk -> {
                    if (StrUtil.isEmpty(chunk)) {
                        return;
                    }
                    aiResponseBuilder.append(chunk);
                    String eventId = streamSessionService.generateEventId(sessionKey);
                    streamSessionService.addEvent(sessionKey, StreamEvent.builder()
                            .eventId(eventId)
                            .eventType("message")
                            .data(JSONUtil.toJsonStr(Map.of("b", chunk)))
                            .build());
                }, error -> {
                    log.error("AI 客服流式响应失败，conversationId={}", conversationId, error);
                    String errorMsg = error.getMessage() != null ? error.getMessage() : "AI 客服响应失败";
                    String eventId = streamSessionService.generateEventId(sessionKey);
                    streamSessionService.addEvent(sessionKey, StreamEvent.builder()
                            .eventId(eventId)
                            .eventType("server_error")
                            .data(JSONUtil.toJsonStr(Map.of("error", errorMsg)))
                            .build());
                    streamSessionService.markError(sessionKey, errorMsg);
                }, () -> {
                    String aiResponse = aiResponseBuilder.toString();
                    if (StrUtil.isNotBlank(aiResponse)) {
                        customerServiceMessageService.addMessage(conversationId, CustomerServiceMessageRoleEnum.AI.getValue(),
                                aiResponse, visitorContext.userId(), visitorContext.visitorKey());
                        customerServiceConversationService.refreshConversation(conversationId, aiResponse,
                                LocalDateTime.now(), false);
                    }
                    String eventId = streamSessionService.generateEventId(sessionKey);
                    streamSessionService.addEvent(sessionKey, StreamEvent.builder()
                            .eventId(eventId)
                            .eventType("end")
                            .data("")
                            .build());
                    streamSessionService.markCompleted(sessionKey);
                });
    }

    private Flux<ServerSentEvent<String>> createFollowFlux(String sessionKey, String lastEventId) {
        AtomicReference<String> lastSentEventId = new AtomicReference<>(lastEventId);
        return Flux.interval(Duration.ofMillis(100))
                .flatMap(tick -> {
                    streamSessionService.updateLastAccessTime(sessionKey);
                    StreamSession session = streamSessionService.getSession(sessionKey);
                    if (session == null) {
                        return Flux.just(ServerSentEvent.<String>builder().event("end").data("").build());
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
                        return Flux.just(ServerSentEvent.<String>builder().event("end").data("").build());
                    }
                    if (session.isHasError()) {
                        String errorMsg = session.getErrorMessage() != null ? session.getErrorMessage() : "未知错误";
                        return Flux.just(ServerSentEvent.<String>builder()
                                .event("server_error")
                                .data(JSONUtil.toJsonStr(Map.of("error", errorMsg)))
                                .build());
                    }
                    return Flux.empty();
                })
                .takeUntil(event -> "end".equals(event.event()) || "server_error".equals(event.event()));
    }

    private String generateSessionKey(Long conversationId, String identityKey, String message) {
        ThrowUtils.throwIf(conversationId == null || conversationId <= 0 || StrUtil.isBlank(identityKey) || StrUtil.isBlank(message),
                ErrorCode.PARAMS_ERROR, "生成 AI 客服会话密钥失败");
        return String.format("customer-service-session-%d-%d", conversationId, (identityKey + message).hashCode());
    }

    private VisitorContext getVisitorContext(HttpServletRequest request) {
        Object loginUser = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (loginUser instanceof User user && user.getId() != null) {
            return new VisitorContext(user.getId(), null, "user:" + user.getId(), user.getId());
        }
        String guestKey = (String) request.getSession().getAttribute(GUEST_VISITOR_KEY);
        if (StrUtil.isBlank(guestKey)) {
            guestKey = "guest:" + IdUtil.simpleUUID();
            request.getSession().setAttribute(GUEST_VISITOR_KEY, guestKey);
        }
        long streamUserId = Math.abs((long) guestKey.hashCode()) + 1L;
        return new VisitorContext(null, guestKey, guestKey, streamUserId);
    }

    private record VisitorContext(Long userId, String visitorKey, String identityKey, Long streamUserId) {
    }
}
