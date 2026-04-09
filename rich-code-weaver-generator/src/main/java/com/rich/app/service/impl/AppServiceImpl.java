package com.rich.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.ai.monitor.MonitorContext;
import com.rich.ai.monitor.MonitorContextHolder;
import com.rich.ai.service.AiCodeGeneratorTypeStrategyService;
import com.rich.app.agent.CodeGenAgentApp;
import com.rich.common.constant.AppOwnershipConstant;
import com.rich.common.constant.StreamEventConstant;
import com.rich.app.langGraph.CodeGenWorkflowApp;
import com.rich.app.mapper.AppMapper;
import com.rich.app.model.StreamEvent;
import com.rich.app.model.StreamSession;
import com.rich.app.service.AppService;
import com.rich.app.service.ChatHistoryService;
import com.rich.app.service.MaterialService;
import com.rich.app.service.StreamSessionService;
import com.rich.app.utils.AIGenerateCodeAndSaveToFileUtils;
import com.rich.client.innerService.InnerCollaboratorService;
import com.rich.client.innerService.InnerScreenshotService;
import com.rich.client.innerService.InnerUserService;
import com.rich.common.constant.AppConstant;
import com.rich.common.constant.UserConstant;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.common.model.DeleteRequest;
import com.rich.model.dto.app.AppAddRequest;
import com.rich.model.dto.app.AppAdminUpdateRequest;
import com.rich.model.dto.app.AppQueryRequest;
import com.rich.model.dto.app.AppUpdateRequest;
import com.rich.model.entity.App;
import com.rich.model.entity.Material;
import com.rich.model.entity.User;
import com.rich.model.enums.ChatHistoryTypeEnum;
import com.rich.model.enums.CodeGeneratorTypeEnum;
import com.rich.model.vo.AppVO;
import com.rich.model.vo.UserVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.HandlerMapping;
import reactor.core.publisher.Flux;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.rich.common.constant.AppConstant.CODE_DEPLOY_ROOT_DIR;
import static com.rich.common.constant.AppConstant.CODE_OUTPUT_ROOT_DIR;

/**
 * AI 产物 服务层实现
 * 实现AI产物的核心业务逻辑，包括代码生成、部署、预览等功能
 *
 * @author DuRuiChi
 * @since 2026-03-10
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    /**
     * 跟随流轮询间隔
     */
    private static final long FOLLOW_POLL_INTERVAL_MILLIS = 100L;

    /**
     * 单次最大素材数量
     */
    private static final int MAX_MATERIAL_COUNT = 20;

    /**
     * 我的产物分页最大大小
     */
    private static final long MAX_MY_APP_PAGE_SIZE = 20L;

    /**
     * 用户相关产物默认条数
     */
    private static final long DEFAULT_USER_RELATED_LIMIT = 8L;

    /**
     * 用户相关产物最大条数
     */
    private static final long MAX_USER_RELATED_LIMIT = 20L;

    /**
     * 未知错误文案
     */
    private static final String UNKNOWN_ERROR_MESSAGE = "未知错误";

    /**
     * 会话 key 模板
     */
    private static final String SESSION_KEY_TEMPLATE = "session-%d-%d-%d";

    /**
     * Agent 模式标识
     */
    private static final String GEN_MODE_AGENT = "agent";

    /**
     * 工作流模式标识
     */
    private static final String GEN_MODE_WORKFLOW = "workflow";

    /**
     * 产物预览路由前缀
     */
    private static final String APP_VIEW_PATH_PREFIX_TEMPLATE = "/generator/app/view/%d";

    /**
     * 根路径默认首页
     */
    private static final String DEFAULT_INDEX_PATH = "/index.html";

    /**
     * 重定向响应头名称
     */
    private static final String LOCATION_HEADER = "Location";

    /**
     * 部署密钥长度
     */
    private static final int DEPLOY_KEY_LENGTH = 8;

    /**
     * HTML 内容类型
     */
    private static final String CONTENT_TYPE_HTML = "text/html; charset=UTF-8";

    /**
     * CSS 内容类型
     */
    private static final String CONTENT_TYPE_CSS = "text/css; charset=UTF-8";

    /**
     * JavaScript 内容类型
     */
    private static final String CONTENT_TYPE_JS = "application/javascript; charset=UTF-8";

    /**
     * PNG 内容类型
     */
    private static final String CONTENT_TYPE_PNG = "image/png";

    /**
     * JPEG 内容类型
     */
    private static final String CONTENT_TYPE_JPEG = "image/jpeg";

    /**
     * 二进制流内容类型
     */
    private static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";

    @Resource
    @Lazy
    private AppService appService;

    @Resource
    private AIGenerateCodeAndSaveToFileUtils aiGenerateCodeAndSaveToFileUtils;

    @Resource
    private ChatHistoryService chatHistoryService;

    // 注入外部截图服务的代理对象
    @DubboReference(check = false)
    private InnerScreenshotService screenshotService;

    // 注入外部用户服务的代理对象
    @DubboReference
    private InnerUserService userService;

    // 注入外部协作者服务的代理对象（用于协作权限校验）
    @DubboReference
    private InnerCollaboratorService collaboratorService;

    @Resource
    private AiCodeGeneratorTypeStrategyService aiCodeGeneratorTypeStrategyService;

    @Resource
    private CodeGenWorkflowApp codeGenWorkflowApp;

    @Resource
    private CodeGenAgentApp codeGenAgentApp;

    @Resource
    private StreamSessionService streamSessionService;

    @Resource
    private MaterialService materialService;

    /**
     * 执行 AI 对话并并生成代码(流式，支持断线重连)
     * 使用工作流分布执行节点模式
     *
     * @param appId       AI 产物id
     * @param userId      用户id
     * @param message     对话消息
     * @param materialIds 选中的素材ID列表（可选）
     * @param isWorkflow  是否开启 Agent 模式（前端参数，暂时保留用于未来 Agent 模式）
     * @param lastEventId 最后接收到的事件ID（用于断线重连）
     * @param reconnect   是否为重连请求
     * @return 代码流
     * @author DuRuiChi
     * @create 2025/12/27
     **/
    @Override
    public Flux<ServerSentEvent<String>> aiChatAndGenerateCodeStreamWithReconnect(Long appId, Long userId, String message, List<Long> materialIds, Boolean isWorkflow, String lastEventId, Boolean reconnect) {
        // 参数校验：确保必要参数有效
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "产物ID无效");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");

        // 生成会话密钥（基于 appId + userId + message hash，确保相同请求复用同一会话）
        String sessionKey = generateSessionKey(appId, userId, message);
        StreamSession existingSession = streamSessionService.getSession(sessionKey);

        // 重连模式：仅跟随已有会话，绝不创建新的生成任务
        if (Boolean.TRUE.equals(reconnect)) {
            if (existingSession != null) {
                // 会话存在，返回跟随流（从lastEventId之后继续推送）
                int eventQueueSize = existingSession.getEventQueue() != null ? existingSession.getEventQueue().size() : 0;
                log.info("重连到现有会话: sessionKey={}, completed={}, hasError={}, eventCount={}",
                        sessionKey, existingSession.isCompleted(), existingSession.isHasError(), eventQueueSize);
                return createFollowFlux(sessionKey, lastEventId);
            } else {
                // 会话不存在（已过期或已被清理），通知前端生成已结束
                log.warn("重连时未找到会话（可能已过期或已被清理）: sessionKey={}", sessionKey);
                return buildEndEventFlux();
            }
        }

        // 如果已有活跃会话（未完成且无错误），直接跟随，避免重复生成（幂等性保证）
        if (hasActiveSession(existingSession)) {
            log.info("发现活跃的未完成会话，直接跟随（避免重复生成）: sessionKey={}", sessionKey);
            return createFollowFlux(sessionKey, lastEventId);
        }

        // 创建新会话（首次请求或会话已完成/出错）
        streamSessionService.createSession(sessionKey, appId, userId);
        log.info("创建新的流式会话: sessionKey={}, appId={}, userId={}", sessionKey, appId, userId);

        // 查询 AI 产物并进行权限校验（所有者或已接受的协作者均可访问）
        App app = getAccessibleAppOrThrow(appId, userId);

        // 获取代码生成类型并校验
        String codeGenTypeValue = app.getCodeGenType();
        ThrowUtils.throwIf(StrUtil.isBlank(codeGenTypeValue), ErrorCode.SYSTEM_ERROR, "产物的代码生成类型为空");

        CodeGeneratorTypeEnum codeGenType = CodeGeneratorTypeEnum.getEnumByValue(codeGenTypeValue);
        ThrowUtils.throwIf(codeGenType == null, ErrorCode.SYSTEM_ERROR, "代码生成类型不合法: " + codeGenTypeValue);

        // 素材处理：将选中的素材内容拼接到消息中
        String finalMessage = appendMaterialPromptIfNecessary(appId, userId, message, materialIds);

        // 保存用户消息到对话历史（仅新建会话时保存，重连不保存）
        // 注意：保存的是原始消息，不包含素材内容（素材内容仅用于AI生成）
        boolean isSaveMsgSuccess = chatHistoryService.addChatMessage(appId, message, ChatHistoryTypeEnum.USER.getValue(), userId);
        ThrowUtils.throwIf(!isSaveMsgSuccess, ErrorCode.OPERATION_ERROR, "保存用户消息失败");

        // 判断是否为二次修改模式：非 AI_STRATEGY 表示类型已确定（即已经过首次生成）
        boolean isModification = codeGenType != CodeGeneratorTypeEnum.AI_STRATEGY;

        // 确定生成模式（agent: Agent自主规划模式, workflow: 工作流分布执行模式）
        boolean isAgentMode = GEN_MODE_AGENT.equals(app.getGenMode());
        String genMode = isAgentMode ? GEN_MODE_AGENT : GEN_MODE_WORKFLOW;

        // 设置监控上下文（在 AI 调用前设置，确保监听器能获取到上下文信息）
        // 注意：必须在流结束时清理上下文，避免 ThreadLocal 内存泄漏
        MonitorContext monitorContext = MonitorContext.builder()
                .userId(userId.toString())
                .appId(appId.toString())
                .genMode(genMode)
                .build();
        MonitorContextHolder.setContext(monitorContext);
        log.debug("设置监控上下文: userId={}, appId={}, genMode={}", userId, appId, genMode);

        // 根据产物的生成模式选择执行路径：Agent 自主规划模式 或 工作流分布执行模式
        Flux<ServerSentEvent<String>> generationFlux;
        if (isAgentMode) {
            log.info("产物 {} 使用 Agent 自主规划模式执行代码生成", appId);
            // 使用包含素材的完整消息
            generationFlux = codeGenAgentApp.executeAgent(finalMessage, appId, chatHistoryService, userId);
        } else {
            log.info("产物 {} 使用工作流分布执行模式执行代码生成", appId);
            // 使用包含素材的完整消息
            generationFlux = codeGenWorkflowApp.executeWorkflow(
                    finalMessage, codeGenType, appId, chatHistoryService, userId, isModification);
        }

        // 独立订阅生成流：AI 生成与客户端连接解耦（核心设计）
        // 客户端断开不影响生成过程，所有事件缓存到会话中，支持断线重连
        generationFlux.subscribe(
                event -> {
                    // 跳过 end/error 事件，这些状态通过会话标记来追踪（避免重复处理）
                    if (StreamEventConstant.EVENT_END.equals(event.event()) || StreamEventConstant.EVENT_ERROR.equals(event.event())) {
                        return;
                    }

                    // 生成唯一事件ID并构建事件对象
                    String eventId = streamSessionService.generateEventId(sessionKey);
                    StreamEvent streamEvent = StreamEvent.builder()
                            .eventId(eventId)
                            .eventType(event.event() != null ? event.event() : StreamEventConstant.EVENT_MESSAGE)  // 默认类型为message
                            .data(event.data())
                            .timestamp(System.currentTimeMillis())  // 记录事件时间戳
                            .build();

                    // 将事件缓存到会话中（线程安全）
                    streamSessionService.addEvent(sessionKey, streamEvent);
                },
                error -> {
                    // 生成过程出错，标记会话错误状态
                    String errorMessage = error.getMessage() != null ? error.getMessage() : UNKNOWN_ERROR_MESSAGE;
                    streamSessionService.markError(sessionKey, errorMessage);
                    log.error("流式会话生成错误: sessionKey={}, error={}", sessionKey, errorMessage, error);

                    // 清理监控上下文（错误场景）
                    // 注意：必须清理，避免 ThreadLocal 内存泄漏
                    try {
                        MonitorContextHolder.clearContext();
                        log.debug("清理监控上下文（错误场景）: userId={}, appId={}", userId, appId);
                    } catch (Exception e) {
                        log.error("清理监控上下文失败", e);
                    }
                },
                () -> {
                    // 生成过程完成，标记会话完成状态
                    streamSessionService.markCompleted(sessionKey);
                    log.info("流式会话生成完成: sessionKey={}", sessionKey);

                    // 清理监控上下文（正常完成场景）
                    // 注意：必须清理，避免 ThreadLocal 内存泄漏
                    try {
                        MonitorContextHolder.clearContext();
                        log.debug("清理监控上下文（完成场景）: userId={}, appId={}", userId, appId);
                    } catch (Exception e) {
                        log.error("清理监控上下文失败", e);
                    }
                }
        );

        // 返回跟随 Flux（从会话缓存中实时读取事件推送给客户端，支持断线重连）
        return createFollowFlux(sessionKey, lastEventId);
    }

    /**
     * 创建跟随 Flux：轮询会话缓存，将事件实时推送给客户端。
     * 无论客户端何时连接/重连，都能从 lastEventId 之后的位置继续接收。
     *
     * @param sessionKey  会话密钥
     * @param lastEventId 客户端最后接收到的事件ID（可为空）
     * @return 跟随 Flux
     */
    private Flux<ServerSentEvent<String>> createFollowFlux(String sessionKey, String lastEventId) {
        // 使用原子引用跟踪最后发送的事件ID（线程安全）
        AtomicReference<String> lastSentEventId = new AtomicReference<>(lastEventId);

        // 每100毫秒轮询一次会话缓存，检查是否有新事件
        return Flux.interval(Duration.ofMillis(FOLLOW_POLL_INTERVAL_MILLIS))
                .flatMap(tick -> {
                    // 获取会话对象
                    StreamSession session = streamSessionService.getSession(sessionKey);
                    if (session == null) {
                        // 会话已被清理（可能已过期），发送结束事件
                        log.debug("会话已被清理，发送结束事件: sessionKey={}", sessionKey);
                        return buildEndEventFlux();
                    }

                    // 获取 lastSentEventId 之后的新事件（支持断点续传）
                    List<StreamEvent> newEvents = streamSessionService.getEventsAfter(
                            sessionKey, lastSentEventId.get());

                    if (!newEvents.isEmpty()) {
                        // 更新最后发送的事件ID（记录当前批次最后一个事件）
                        StreamEvent lastEvent = newEvents.get(newEvents.size() - 1);
                        lastSentEventId.set(lastEvent.getEventId());

                        // 将新事件转换为SSE格式并推送给客户端
                        return Flux.fromIterable(newEvents)
                                .map(this::buildSseEvent);
                    }

                    // 没有新事件时，检查会话是否已结束
                    if (session.isCompleted()) {
                        // 再次检查是否有遗漏的事件（防止竞态条件：完成标记和最后事件之间的时间差）
                        List<StreamEvent> remainingEvents = streamSessionService.getEventsAfter(
                                sessionKey, lastSentEventId.get());
                        if (!remainingEvents.isEmpty()) {
                            // 发现遗漏事件，先推送这些事件
                            StreamEvent lastRemaining = remainingEvents.get(remainingEvents.size() - 1);
                            lastSentEventId.set(lastRemaining.getEventId());
                            return Flux.fromIterable(remainingEvents)
                                    .map(this::buildSseEvent);
                        }
                        // 所有事件已推送完毕，发送结束事件
                        log.debug("会话已完成，发送结束事件: sessionKey={}", sessionKey);
                        return buildEndEventFlux();
                    }

                    // 检查会话是否出错
                    if (session.isHasError()) {
                        String errorMsg = session.getErrorMessage() != null ? session.getErrorMessage() : UNKNOWN_ERROR_MESSAGE;
                        log.warn("会话出错，发送错误事件: sessionKey={}, error={}", sessionKey, errorMsg);
                        return Flux.just(buildErrorSseEvent(errorMsg));
                    }

                    // 生成仍在进行中，暂无新事件，返回空流（继续轮询）
                    return Flux.empty();
                })
                // 收到 end 或 error 事件后自动终止流（避免无限轮询）
                .takeUntil(event -> StreamEventConstant.EVENT_END.equals(event.event())
                        || StreamEventConstant.EVENT_ERROR.equals(event.event()));
    }

    /**
     * 生成会话密钥（基于 appId + userId + message hash）
     * 确保相同的请求参数生成相同的会话 ID
     */
    private String generateSessionKey(Long appId, Long userId, String message) {
        // 参数校验（内部方法，双重保护）
        if (appId == null || appId <= 0 || userId == null || userId <= 0 || StrUtil.isBlank(message)) {
            log.error("生成会话密钥失败：参数无效 - appId={}, userId={}, message={}", appId, userId,
                    message != null ? message.substring(0, Math.min(message.length(), 20)) : "null");
            throw new IllegalArgumentException("生成会话密钥的参数不能为空");
        }

        // 生成会话密钥：基于 appId + userId + message.hashCode()
        // 相同的请求参数生成相同的会话ID，实现幂等性（避免重复生成）
        int messageHash = message.hashCode();
        String sessionKey = String.format(SESSION_KEY_TEMPLATE, appId, userId, messageHash);

        log.debug("生成会话密钥: sessionKey={}, appId={}, userId={}, messageHash={}",
                sessionKey, appId, userId, messageHash);
        return sessionKey;
    }

    /**
     * 预览指定产物
     *
     * @param appId   产物 ID
     * @param request 请求对象
     * @return org.springframework.http.ResponseEntity<jakarta.annotation.Resource> 产物资源
     * @author DuRuiChi
     * @create 2025/12/8
     **/
    @Override
    public ResponseEntity<FileSystemResource> serverStaticResource(Long appId, HttpServletRequest request) {
        try {
            // 参数校验：确保产物ID有效
            if (appId == null || appId <= 0) {
                log.warn("预览产物失败：产物ID无效 - appId={}", appId);
                return ResponseEntity.badRequest().build();
            }

            // 查询产物信息
            App app = appService.getById(appId);
            if (app == null) {
                log.warn("预览产物失败：产物不存在 - appId={}", appId);
                return ResponseEntity.notFound().build();
            }

            // viewKey：所有模式均使用 codeGenType_appId 命名（Agent/工作流模式一致）
            String codeGenType = app.getCodeGenType();
            String viewKey = codeGenType + "_" + appId;

            // 获取原始请求路径（从请求属性中提取）
            String resourcePath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            if (resourcePath == null) {
                log.warn("预览产物失败：无法获取资源路径 - appId={}", appId);
                return ResponseEntity.badRequest().build();
            }

            // 提取资源文件路径（去除前缀 /generator/app/view/{appId}）
            String pathPrefix = String.format(APP_VIEW_PATH_PREFIX_TEMPLATE, appId);
            resourcePath = resourcePath.substring(pathPrefix.length());

            // 当路径为空时自动添加斜杠，避免路径解析问题（重定向到根路径）
            if (resourcePath.isEmpty()) {
                return buildRedirectResponse(request.getRequestURI() + "/");
            }

            // 默认访问 index.html（根路径）
            if ("/".equals(resourcePath)) {
                resourcePath = DEFAULT_INDEX_PATH;
            }

            // vue_project 类型优先服务 dist/（构建产物），未构建时回退到根目录（兼容 Agent 模式构建中的预览）
            String filePath = resolvePreviewFilePath(viewKey, codeGenType, resourcePath);
            File file = new File(filePath);

            // 安全检查：防止路径穿越攻击
            String canonicalPath = file.getCanonicalPath();
            String expectedBasePath = new File(CODE_OUTPUT_ROOT_DIR + File.separator + viewKey).getCanonicalPath();
            if (!canonicalPath.startsWith(expectedBasePath)) {
                log.warn("预览产物失败：检测到路径穿越攻击 - appId={}, path={}", appId, resourcePath);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 检查文件是否存在
            if (!file.exists() || !file.isFile()) {
                log.debug("预览产物失败：文件不存在 - appId={}, filePath={}", appId, filePath);
                return ResponseEntity.notFound().build();
            }

            // 返回产物文件资源
            FileSystemResource fileSystemResource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header("Content-Type", getContentTypeWithCharset(filePath))
                    .body(fileSystemResource);
        } catch (Exception e) {
            log.error("预览产物失败：系统错误 - appId={}, error={}", appId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    /**
     * 部署产物
     *
     * @param appId
     * @param loginUser
     * @return java.lang.String
     * @author DuRuiChi
     * @create 2025/12/27
     **/
    @Override
    public String deployApp(Long appId, User loginUser) {
        // 参数校验：确保产物ID和登录用户有效
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "产物ID无效");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        ThrowUtils.throwIf(loginUser.getId() == null || loginUser.getId() <= 0, ErrorCode.NOT_LOGIN_ERROR, "用户ID无效");

        // 查询产物信息并校验
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "产物不存在");

        // 权限校验：仅产物所有者可部署
        if (!app.getUserId().equals(loginUser.getId())) {
            log.warn("部署产物失败：权限不足 - appId={}, userId={}, ownerId={}",
                    appId, loginUser.getId(), app.getUserId());
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "这不是您的产物，无法部署");
        }

        // 获取或生成部署密钥（deployKey）
        String deployKey = app.getDeployKey();
        if (StrUtil.isBlank(deployKey)) {
            // 首次部署，生成8位随机部署密钥
            deployKey = RandomUtil.randomString(DEPLOY_KEY_LENGTH);
            log.info("生成新的部署密钥: appId={}, deployKey={}", appId, deployKey);
        }
        // 复制文件：从生成目录到部署目录
        try {
            // 构建代码输出文件夹路径
            File outputDir = getOutputDir(app);

            // 校验代码输出文件夹是否存在
            validateDeploySourceDir(outputDir, appId);

            // 构建代码部署文件夹路径
            File deployDir = getDeployDir(deployKey);

            // 复制目录内容（执行覆盖，支持重复部署）
            FileUtil.copyContent(outputDir, deployDir, true);
            log.info("产物文件复制成功: appId={}, from={}, to={}",
                    appId, outputDir.getAbsolutePath(), deployDir.getAbsolutePath());
        } catch (BusinessException e) {
            // 业务异常直接抛出
            throw e;
        } catch (Exception e) {
            // 其他异常包装为业务异常
            log.error("部署产物失败：文件复制异常 - appId={}, error={}", appId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "产物转储失败：" + e.getMessage());
        }
        // 更新产物信息（记录部署密钥和部署时间）
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());

        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "产物信息更新失败");

        // 构建部署访问 URL
        String appUrl = String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
        log.info("产物部署成功: appId={}, deployKey={}, appUrl={}", appId, deployKey, appUrl);

        // 异步生成产物截图并更新封面（不阻塞部署流程）
        generateAppScreenshotAsync(appId, appUrl);

        return appUrl;
    }

    /**
     * 异步生成产物截图并更新封面
     *
     * @param appId  产物ID
     * @param appUrl 产物访问URL
     */
    private void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 开启 JDK-21 的虚拟线程，避免阻塞主流程（异步执行截图任务）
        Thread.ofVirtual().start(() -> {
            try {
                log.info("开始异步生成产物截图: appId={}, appUrl={}", appId, appUrl);

                // 调用截图服务生成截图并上传到云存储
                String screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);

                // 校验截图URL是否有效
                if (StrUtil.isBlank(screenshotUrl)) {
                    log.error("生成产物截图失败：截图URL为空 - appId={}", appId);
                    return;
                }

                // 更新产物封面字段
                App updateApp = new App();
                updateApp.setId(appId);
                updateApp.setCover(screenshotUrl);

                boolean updated = this.updateById(updateApp);
                if (updated) {
                    log.info("产物封面更新成功: appId={}, screenshotUrl={}", appId, screenshotUrl);
                } else {
                    log.error("产物封面更新失败: appId={}", appId);
                }
            } catch (Exception e) {
                // 异步任务异常不影响主流程，仅记录日志
                log.error("异步生成产物截图失败: appId={}, error={}", appId, e.getMessage(), e);
            }
        });
    }


    /**
     * 构建代码输出文件夹
     *
     * @param app 产物
     * @return java.io.File
     **/
    @Override
    public File getOutputDir(App app) {
        String codeGenType = app.getCodeGenType();
        // 所有模式（Agent/工作流）统一使用 codeGenType_appId 目录
        String outputDirName = codeGenType + "_" + app.getId();
        if (codeGenType.equals(CodeGeneratorTypeEnum.VUE_PROJECT.getValue())) {
            outputDirName += File.separator + "dist";
        }
        return new File(CODE_OUTPUT_ROOT_DIR + File.separator + outputDirName);
    }

    /**
     * 构建用于删除的代码输出文件夹（没有 dist 直接删除整个目录）
     *
     * @param app 产物
     * @return java.io.File
     **/
    @Override
    public File getDelOutputDir(App app) {
        // 所有模式（Agent/工作流）统一使用 codeGenType_appId 目录
        String outputDirName = app.getCodeGenType() + "_" + app.getId();
        return new File(CODE_OUTPUT_ROOT_DIR + File.separator + outputDirName);
    }

    /**
     * 构建代码部署文件夹
     *
     * @param deployKey 部署密钥
     * @return java.io.File
     **/
    @Override
    public File getDeployDir(String deployKey) {
        return new File(CODE_DEPLOY_ROOT_DIR + File.separator + deployKey);
    }

    /**
     * 根据文件扩展名返回带字符编码的 Content-Type
     *
     * @param filePath 文件路径
     * @return java.lang.String 带字符编码的 Content-Type
     **/
    private String getContentTypeWithCharset(String filePath) {
        if (filePath.endsWith(".html")) return CONTENT_TYPE_HTML;
        if (filePath.endsWith(".css")) return CONTENT_TYPE_CSS;
        if (filePath.endsWith(".js")) return CONTENT_TYPE_JS;
        if (filePath.endsWith(".png")) return CONTENT_TYPE_PNG;
        if (filePath.endsWith(".jpg")) return CONTENT_TYPE_JPEG;
        return CONTENT_TYPE_OCTET_STREAM;
    }

    /**
     * 构造重定向响应
     *
     * @param redirectUrl 重定向地址
     * @return 重定向响应
     */
    private ResponseEntity<FileSystemResource> buildRedirectResponse(String redirectUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(LOCATION_HEADER, redirectUrl);
        return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
    }

    /**
     * 解析预览文件绝对路径
     * Vue 项目优先读取 dist 目录，未构建时回退到根目录
     *
     * @param viewKey 预览目录 key
     * @param codeGenType 代码生成类型
     * @param resourcePath 资源相对路径
     * @return 文件绝对路径
     */
    private String resolvePreviewFilePath(String viewKey, String codeGenType, String resourcePath) {
        String normalizedPath = resourcePath.replace("/", File.separator);
        String basePath = CODE_OUTPUT_ROOT_DIR + File.separator + viewKey;
        if (CodeGeneratorTypeEnum.VUE_PROJECT.getValue().equals(codeGenType)) {
            File distFile = new File(basePath + File.separator + "dist" + normalizedPath);
            File rootFile = new File(basePath + normalizedPath);
            return distFile.exists() ? distFile.getAbsolutePath() : rootFile.getAbsolutePath();
        }
        return basePath + normalizedPath;
    }

    /**
     * 校验部署源码目录是否可用
     *
     * @param outputDir 源码目录
     * @param appId 产物 ID
     */
    private void validateDeploySourceDir(File outputDir, Long appId) {
        if (!outputDir.exists()) {
            log.warn("部署产物失败：源码目录不存在 - appId={}, outputDir={}", appId, outputDir.getAbsolutePath());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "未检测到源码，无法部署，请先生成源码");
        }
        if (!outputDir.isDirectory()) {
            log.error("部署产物失败：源码路径不是目录 - appId={}, outputDir={}", appId, outputDir.getAbsolutePath());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "源码路径异常，无法部署");
        }
    }

    /**
     * 删除目录（如果存在）
     *
     * @param targetDir 目标目录
     * @param appId 产物 ID
     * @param dirLabel 目录名称
     * @param failureMessage 删除失败提示
     */
    private void deleteDirectoryIfPresent(File targetDir, long appId, String dirLabel, String failureMessage) {
        if (targetDir.exists() && targetDir.isDirectory()) {
            boolean deleted = FileUtils.deleteQuietly(targetDir);
            if (deleted) {
                log.info("删除产物{}成功: appId={}, path={}", dirLabel, appId, targetDir.getAbsolutePath());
            } else {
                log.error("删除产物{}失败: appId={}, path={}", dirLabel, appId, targetDir.getAbsolutePath());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, failureMessage);
            }
        } else {
            log.info("产物{}不存在，跳过删除: appId={}", dirLabel, appId);
        }
    }

    /**
     * 获取当前登录用户，不存在时直接抛异常
     *
     * @param request 请求对象
     * @return 当前登录用户
     */
    private User getLoginUserOrThrow(HttpServletRequest request) {
        User loginUser = InnerUserService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        return loginUser;
    }

    /**
     * 根据 ID 查询产物，不存在时抛异常
     *
     * @param appId 产物 ID
     * @return 产物实体
     */
    private App getAppByIdOrThrow(Long appId) {
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "产物不存在");
        return app;
    }

    /**
     * 批量获取 AI 产物 VO 列表
     *
     * @param appList AI 产物列表
     * @return java.util.List<com.rich.app.model.vo.AppVO>
     * @author DuRuiChi
     * @create 2025/12/8
     **/
    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        // 空列表处理
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 提取所有用户ID（避免N+1查询）
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());

        // 批量查询用户信息并构建映射
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));

        // 转换 AI 产物实体为VO对象
        return appList.stream().map(app -> {
            // 获取基础VO信息
            AppVO appVO = getAppVO(app);
            // 设置关联用户信息
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    /**
     * 构造 AI 产物查询条件
     *
     * @param appQueryRequest AI 产物查询请求
     * @return com.mybatisflex.core.query.QueryWrapper  查询条件
     * @author DuRuiChi
     * @create 2025/12/8
     **/
    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        // 请求参数校验
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        // 解构请求参数
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();

        // 构建查询条件
        return QueryWrapper.create()
                // 精确匹配
                .eq("id", id)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                // 模糊查询
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                // 排序
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    /**
     * 添加 AI 产物
     *
     * @param appAddRequest AI 产物添加请求
     * @param request       请求
     * @return java.lang.Long
     * @author DuRuiChi
     * @create 2025/12/8
     **/
    @Override
    public Long addApp(AppAddRequest appAddRequest, HttpServletRequest request) {
        // 参数校验：确保请求对象有效
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");

        // 校验初始化提示词
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化提示词不能为空");

        // 校验代码生成类型
        CodeGeneratorTypeEnum generatorType = appAddRequest.getGeneratorType();
        ThrowUtils.throwIf(generatorType == null, ErrorCode.PARAMS_ERROR, "代码生成类型不能为空");

        // 获取当前登录用户并校验
        User loginUser = getLoginUserOrThrow(request);

        // 创建 AI 产物实体
        App app = new App();
        // 复制请求属性到实体（基础属性）
        BeanUtil.copyProperties(appAddRequest, app);

        // 设置用户关联（产物所有者）
        app.setUserId(loginUser.getId());

        // 生成 AI 产物名称（截取提示词前30字符，避免名称过长）
        int nameLength = Math.min(initPrompt.length(), 30);
        String appName = initPrompt.substring(0, nameLength);
        app.setAppName(appName);

        // 设置生成模式（workflow / agent），默认为 workflow
        String genMode = StrUtil.blankToDefault(appAddRequest.getGenMode(), GEN_MODE_WORKFLOW);
        app.setGenMode(genMode);

        // 设置代码生成策略
        // Agent 模式固定使用 vue_project（工具默认写入该目录，构建和预览均依赖此类型）
        // 工作流模式保存用户选择的策略，由工作流策略节点智能选择最优方案
        if (GEN_MODE_AGENT.equals(genMode)) {
            app.setCodeGenType(CodeGeneratorTypeEnum.VUE_PROJECT.getValue());
        } else {
            app.setCodeGenType(generatorType.getValue());
        }

        // 设置默认封面图片
        app.setCover(AppConstant.APP_COVER);

        // 保存 AI 产物数据到数据库
        boolean saveSuccess = appService.save(app);
        ThrowUtils.throwIf(!saveSuccess, ErrorCode.OPERATION_ERROR, "保存产物失败");

        log.info("创建AI产物成功: appId={}, userId={}, appName={}, codeGenType={}, genMode={}",
                app.getId(), loginUser.getId(), appName, app.getCodeGenType(), genMode);

        return app.getId();
    }

    /**
     * 删除 AI 产物
     *
     * @param deleteRequest 删除请求
     * @param request       请求
     * @return java.lang.Boolean
     * @author DuRuiChi
     * @create 2025/12/8
     **/
    @Override
    public Boolean deleteApp(DeleteRequest deleteRequest, HttpServletRequest request) {
        // 参数校验：确保删除请求有效
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR, "删除请求参数为空");

        // 获取当前登录用户并校验
        User loginUser = getLoginUserOrThrow(request);

        // 解析 AI 产物ID
        long appId = deleteRequest.getId();
        ThrowUtils.throwIf(appId <= 0, ErrorCode.PARAMS_ERROR, "产物ID无效");

        // 查询目标 AI 产物并校验存在性
        App targetApp = getAppByIdOrThrow(appId);

        // 权限校验：仅产物所有者或管理员可删除
        boolean isOwner = targetApp.getUserId().equals(loginUser.getId());
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        if (!isOwner && !isAdmin) {
            log.warn("删除产物失败：权限不足 - appId={}, userId={}, ownerId={}",
                    appId, loginUser.getId(), targetApp.getUserId());
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权删除该产物");
        }

        // 删除关联的历史对话消息记录（级联删除）
        boolean deleteChatHistory = chatHistoryService.deleteByAppId(appId);
        log.info("删除产物对话历史: appId={}, success={}", appId, deleteChatHistory);

        // 构建输出目录路径
        File outputDir = appService.getDelOutputDir(targetApp);
        deleteDirectoryIfPresent(outputDir, appId, "输出目录", "输出目录删除失败");

        // 构建部署目录路径
        String deployKey = targetApp.getDeployKey();
        if (StrUtil.isNotBlank(deployKey)) {
            File deployDir = appService.getDeployDir(deployKey);
            deleteDirectoryIfPresent(deployDir, appId, "部署目录", "部署目录删除失败");
        }

        // 删除产物数据库记录
        boolean deleteSuccess = appService.removeById(appId);
        ThrowUtils.throwIf(!deleteSuccess, ErrorCode.OPERATION_ERROR, "删除产物记录失败");

        log.info("删除AI产物成功: appId={}, userId={}", appId, loginUser.getId());
        return true;
    }

    /**
     * 更新 AI 产物
     *
     * @param appUpdateRequest AI 产物更新请求
     * @param request          请求
     * @return java.lang.Boolean
     * @author DuRuiChi
     * @create 2025/12/8
     **/
    @Override
    public Boolean updateApp(AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        // 参数校验：确保更新请求有效
        ThrowUtils.throwIf(appUpdateRequest == null, ErrorCode.PARAMS_ERROR, "更新请求参数为空");

        // 获取当前登录用户并校验
        User loginUser = getLoginUserOrThrow(request);

        // 解析 AI 产物ID
        long appId = appUpdateRequest.getId();
        ThrowUtils.throwIf(appId <= 0, ErrorCode.PARAMS_ERROR, "产物ID无效");

        // 校验产物名称
        String newAppName = appUpdateRequest.getAppName();
        ThrowUtils.throwIf(StrUtil.isBlank(newAppName), ErrorCode.PARAMS_ERROR, "产物名称不能为空");

        // 查询现有 AI 产物并校验存在性
        App existingApp = getAppByIdOrThrow(appId);

        // 权限校验：仅产物所有者可更新
        if (!existingApp.getUserId().equals(loginUser.getId())) {
            log.warn("更新产物失败：权限不足 - appId={}, userId={}, ownerId={}",
                    appId, loginUser.getId(), existingApp.getUserId());
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权更新该产物");
        }

        // 创建更新对象（仅更新允许修改的字段）
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setAppName(newAppName);
        updateApp.setEditTime(LocalDateTime.now());  // 记录更新时间

        // 执行更新操作
        boolean updateSuccess = appService.updateById(updateApp);
        ThrowUtils.throwIf(!updateSuccess, ErrorCode.OPERATION_ERROR, "更新产物失败");

        log.info("更新AI产物成功: appId={}, userId={}, newAppName={}", appId, loginUser.getId(), newAppName);
        return true;
    }

    /**
     * 管理员更新 AI 产物
     *
     * @param appAdminUpdateRequest AI 产物管理员更新请求
     * @return java.lang.Boolean
     * @author DuRuiChi
     * @create 2025/12/8
     **/
    @Override
    public Boolean updateAppByAdmin(AppAdminUpdateRequest appAdminUpdateRequest) {
        // 解析 AI 产物ID
        long id = appAdminUpdateRequest.getId();

        // 查询现有 AI 产物
        getAppByIdOrThrow(id);

        // 复制管理员更新属性
        App app = toAdminUpdateApp(appAdminUpdateRequest);

        // 执行管理员更新
        boolean result = appService.updateById(app);
        // 校验更新结果
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

    /**
     * 管理员获取 AI 产物分页
     *
     * @param appQueryRequest AI 产物查询请求
     * @return com.mybatisflex.core.paginate.Page<com.rich.app.model.vo.AppVO>
     * @author DuRuiChi
     * @create 2025/12/8
     **/
    @Override
    public Page<AppVO> listAppVOByPageByAdmin(AppQueryRequest appQueryRequest) {
        // 获取分页参数
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        // 构建查询条件
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        // 执行分页查询
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        return buildAppVOPage(pageNum, pageSize, appPage);
    }

    /**
     * 获取我的 AI 产物分页
     *
     * @param appQueryRequest AI 产物查询请求
     * @param request         请求
     * @return com.mybatisflex.core.paginate.Page<com.rich.app.model.vo.AppVO>
     * @author DuRuiChi
     * @create 2025/12/8
     **/
    @Override
    public Page<AppVO> listMyAppVOByPage(AppQueryRequest appQueryRequest, HttpServletRequest request) {
        User loginUser = InnerUserService.getLoginUser(request);
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > MAX_MY_APP_PAGE_SIZE, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个 AI 产物");
        long pageNum = appQueryRequest.getPageNum();

        List<Long> collaboratedAppIds = listCollaboratedAppIds(loginUser.getId());
        Map<Long, App> appMap = buildAccessibleAppMap(loginUser.getId(), collaboratedAppIds);

        List<App> filteredApps = appMap.values().stream()
                .filter(app -> matchesMyAppQuery(app, appQueryRequest))
                .sorted(buildMyAppComparator(appQueryRequest))
                .collect(Collectors.toList());

        int fromIndex = (int) Math.max(0, (pageNum - 1) * pageSize);
        int toIndex = (int) Math.min(filteredApps.size(), fromIndex + pageSize);
        List<App> pageRecords = fromIndex >= filteredApps.size()
                ? new ArrayList<>()
                : filteredApps.subList(fromIndex, toIndex);

        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, filteredApps.size());
        List<AppVO> appVOList = appService.getAppVOList(pageRecords);
        Set<Long> collaboratedAppIdSet = collaboratedAppIds.stream().collect(Collectors.toSet());
        fillOwnershipType(appVOList, loginUser.getId(), collaboratedAppIdSet);
        appVOPage.setRecords(appVOList);
        return appVOPage;
    }

    @Override
    public List<AppVO> listUserRelatedApps(Long userId, long limit) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID无效");
        long safeLimit = limit <= 0 ? DEFAULT_USER_RELATED_LIMIT : Math.min(limit, MAX_USER_RELATED_LIMIT);

        List<Long> collaboratedAppIds = listCollaboratedAppIds(userId);
        Map<Long, App> appMap = buildAccessibleAppMap(userId, collaboratedAppIds);

        List<App> filteredApps = appMap.values().stream()
                .sorted(Comparator
                        .comparing(App::getUpdateTime, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(App::getCreateTime, Comparator.nullsLast(LocalDateTime::compareTo))
                        .reversed())
                .limit(safeLimit)
                .collect(Collectors.toList());

        Set<Long> collaboratedAppIdSet = collaboratedAppIds.stream().collect(Collectors.toSet());
        List<AppVO> appVOList = appService.getAppVOList(filteredApps);
        fillOwnershipType(appVOList, userId, collaboratedAppIdSet);
        return appVOList;
    }

    /**
     * 判断当前流式会话是否仍处于活跃状态
     *
     * @param session 流式会话
     * @return 是否活跃
     */
    private boolean hasActiveSession(StreamSession session) {
        return session != null && !session.isCompleted() && !session.isHasError();
    }

    /**
     * 构造仅包含结束事件的 Flux
     *
     * @return 结束事件流
     */
    private Flux<ServerSentEvent<String>> buildEndEventFlux() {
        return Flux.just(buildEndSseEvent());
    }

    /**
     * 构造结束 SSE 事件
     *
     * @return 结束事件
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
     * @return 错误事件
     */
    private ServerSentEvent<String> buildErrorSseEvent(String errorMsg) {
        return ServerSentEvent.<String>builder()
                .event(StreamEventConstant.EVENT_ERROR)
                .data(JSONUtil.toJsonStr(Map.of(StreamEventConstant.DATA_ERROR_KEY, errorMsg)))
                .build();
    }

    /**
     * 将流式事件转换为 SSE 事件
     *
     * @param event 流式事件
     * @return SSE 事件
     */
    private ServerSentEvent<String> buildSseEvent(StreamEvent event) {
        return ServerSentEvent.<String>builder()
                .id(event.getEventId())
                .event(event.getEventType())
                .data(event.getData())
                .build();
    }

    /**
     * 查询用户参与的协作产物 ID 列表
     *
     * @param userId 用户 ID
     * @return 协作产物 ID 列表
     */
    private List<Long> listCollaboratedAppIds(Long userId) {
        List<Long> collaboratedAppIds = collaboratorService.listCollaboratedAppIds(userId);
        return collaboratedAppIds == null ? new ArrayList<>() : collaboratedAppIds;
    }

    /**
     * 构建用户可访问的产物映射
     * 包含自己的产物和协作中的产物，且按插入顺序去重
     *
     * @param userId 用户 ID
     * @param collaboratedAppIds 协作产物 ID 列表
     * @return 可访问产物映射
     */
    private Map<Long, App> buildAccessibleAppMap(Long userId, List<Long> collaboratedAppIds) {
        List<App> ownedApps = this.list(QueryWrapper.create().where("userId = ?", userId));
        List<App> collaboratedApps = CollUtil.isEmpty(collaboratedAppIds)
                ? new ArrayList<>()
                : this.list(QueryWrapper.create().in("id", collaboratedAppIds));

        Map<Long, App> appMap = new LinkedHashMap<>();
        ownedApps.forEach(app -> appMap.put(app.getId(), app));
        collaboratedApps.forEach(app -> appMap.putIfAbsent(app.getId(), app));
        return appMap;
    }

    /**
     * 为产物视图对象补充归属类型
     *
     * @param appVOList 产物视图对象列表
     * @param userId 当前用户 ID
     * @param collaboratedAppIdSet 协作产物 ID 集合
     */
    private void fillOwnershipType(List<AppVO> appVOList, Long userId, Set<Long> collaboratedAppIdSet) {
        appVOList.forEach(appVO -> {
            if (userId.equals(appVO.getUserId())) {
                appVO.setOwnershipType(AppOwnershipConstant.OWNERSHIP_MINE);
            } else if (appVO.getId() != null && collaboratedAppIdSet.contains(appVO.getId())) {
                appVO.setOwnershipType(AppOwnershipConstant.OWNERSHIP_COLLABORATOR);
            }
        });
    }

    /**
     * 查询当前用户可访问的产物
     * 所有者或已接受的协作者都可以继续生成
     *
     * @param appId 产物 ID
     * @param userId 用户 ID
     * @return 可访问的产物实体
     */
    private App getAccessibleAppOrThrow(Long appId, Long userId) {
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "产物不存在");
        boolean isOwner = app.getUserId().equals(userId);
        boolean isCollaborator = !isOwner && collaboratorService.isCollaborator(appId, userId);
        ThrowUtils.throwIf(!isOwner && !isCollaborator, ErrorCode.FORBIDDEN_ERROR, "无权访问该产物");
        return app;
    }

    /**
     * 将可访问素材内容追加到原始消息后面
     * 如果没有可用素材，则直接返回原始消息
     *
     * @param appId 产物 ID
     * @param userId 用户 ID
     * @param message 原始消息
     * @param materialIds 素材 ID 列表
     * @return 追加素材后的消息
     */
    private String appendMaterialPromptIfNecessary(Long appId, Long userId, String message, List<Long> materialIds) {
        if (CollUtil.isEmpty(materialIds)) {
            return message;
        }

        // 限制素材数量，避免消息过长
        ThrowUtils.throwIf(materialIds.size() > MAX_MATERIAL_COUNT, ErrorCode.PARAMS_ERROR, "一次最多选择20个素材");

        // 查询素材列表（只查询用户有权访问的素材：自己的 + 公开的）
        List<Material> materials = materialService.listByIds(materialIds);
        List<Material> accessibleMaterials = materials.stream()
                .filter(m -> m.getUserId().equals(userId) || m.getIsPublic() == 1)
                .toList();

        if (CollUtil.isEmpty(accessibleMaterials)) {
            return message;
        }

        // 格式化素材内容并拼接到消息末尾
        String materialPrompt = materialService.formatMaterialsForPrompt(accessibleMaterials);

        // 异步增加素材使用次数
        List<Long> usedMaterialIds = accessibleMaterials.stream()
                .map(Material::getId)
                .toList();
        materialService.batchIncrementUseCount(usedMaterialIds);

        log.info("产物 {} 对话使用了 {} 个素材", appId, accessibleMaterials.size());
        return message + materialPrompt;
    }

    /**
     * 判断产物是否匹配“我的产物”筛选条件
     *
     * @param app 产物实体
     * @param appQueryRequest 查询请求
     * @return 是否匹配
     */
    private boolean matchesMyAppQuery(App app, AppQueryRequest appQueryRequest) {
        if (appQueryRequest.getId() != null && !appQueryRequest.getId().equals(app.getId())) {
            return false;
        }
        if (StrUtil.isNotBlank(appQueryRequest.getAppName())
                && !StrUtil.containsIgnoreCase(StrUtil.nullToEmpty(app.getAppName()), appQueryRequest.getAppName())) {
            return false;
        }
        if (StrUtil.isNotBlank(appQueryRequest.getCover())
                && !StrUtil.containsIgnoreCase(StrUtil.nullToEmpty(app.getCover()), appQueryRequest.getCover())) {
            return false;
        }
        if (StrUtil.isNotBlank(appQueryRequest.getInitPrompt())
                && !StrUtil.containsIgnoreCase(StrUtil.nullToEmpty(app.getInitPrompt()), appQueryRequest.getInitPrompt())) {
            return false;
        }
        if (StrUtil.isNotBlank(appQueryRequest.getCodeGenType())
                && !appQueryRequest.getCodeGenType().equals(app.getCodeGenType())) {
            return false;
        }
        if (StrUtil.isNotBlank(appQueryRequest.getDeployKey())
                && !StrUtil.containsIgnoreCase(StrUtil.nullToEmpty(app.getDeployKey()), appQueryRequest.getDeployKey())) {
            return false;
        }
        return appQueryRequest.getPriority() == null || appQueryRequest.getPriority().equals(app.getPriority());
    }

    /**
     * 构建“我的产物”列表排序器
     *
     * @param appQueryRequest 查询请求
     * @return 排序器
     */
    private Comparator<App> buildMyAppComparator(AppQueryRequest appQueryRequest) {
        String sortField = appQueryRequest.getSortField();
        boolean asc = "ascend".equalsIgnoreCase(appQueryRequest.getSortOrder())
                || "asc".equalsIgnoreCase(appQueryRequest.getSortOrder());
        Comparator<App> comparator;
        if ("appName".equals(sortField)) {
            comparator = Comparator.comparing(app -> StrUtil.nullToEmpty(app.getAppName()), String.CASE_INSENSITIVE_ORDER);
        } else if ("updateTime".equals(sortField)) {
            comparator = Comparator.comparing(App::getUpdateTime, Comparator.nullsLast(LocalDateTime::compareTo));
        } else if ("priority".equals(sortField)) {
            comparator = Comparator.comparing(App::getPriority, Comparator.nullsLast(Integer::compareTo));
        } else {
            comparator = Comparator.comparing(App::getCreateTime, Comparator.nullsLast(LocalDateTime::compareTo));
        }
        return asc ? comparator : comparator.reversed();
    }

    /**
     * 获取星标 AI 产物分页
     *
     * @param appQueryRequest AI 产物查询请求
     * @return com.mybatisflex.core.paginate.Page<com.rich.app.model.vo.AppVO>
     * @author DuRuiChi
     * @create 2025/12/8
     **/
    @Override
    public Page<AppVO> listStarAppVOByPage(AppQueryRequest appQueryRequest) {
        // 分页参数处理
        long pageSize = appQueryRequest.getPageSize();
        // 分页大小限制
        ThrowUtils.throwIf(pageSize > MAX_MY_APP_PAGE_SIZE, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个 AI 产物");
        long pageNum = appQueryRequest.getPageNum();

        // 设置星标 AI 产物过滤条件
        appQueryRequest.setPriority(AppConstant.STAR_APP_PRIORITY);

        // 构建查询条件
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        // 执行分页查询
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        return buildAppVOPage(pageNum, pageSize, appPage);
    }

    /**
     * 将管理员更新请求转换为待更新产物实体
     *
     * @param appAdminUpdateRequest 管理员更新请求
     * @return 待更新产物实体
     */
    private App toAdminUpdateApp(AppAdminUpdateRequest appAdminUpdateRequest) {
        App app = new App();
        BeanUtil.copyProperties(appAdminUpdateRequest, app);
        app.setEditTime(LocalDateTime.now());
        return app;
    }

    /**
     * 根据实体分页结果构造 VO 分页结果
     *
     * @param pageNum 页号
     * @param pageSize 页大小
     * @param appPage 实体分页结果
     * @return VO 分页结果
     */
    private Page<AppVO> buildAppVOPage(long pageNum, long pageSize, Page<App> appPage) {
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return appVOPage;
    }

    /**
     * 获取 AI 产物 VO 封装类
     *
     * @param app AI 产物实体类
     * @return com.rich.app.model.vo.AppVO
     * @author DuRuiChi
     * @create 2025/12/8
     **/
    @Override
    public AppVO getAppVO(App app) {
        // 空值校验
        if (app == null) {
            return null;
        }
        // 创建VO对象
        AppVO appVO = new AppVO();
        // 复制属性到VO对象
        BeanUtil.copyProperties(app, appVO);
        // 获取关联用户ID
        Long userId = app.getUserId();
        // 关联用户数据
        if (userId != null) {
            // 查询用户信息
            User user = userService.getById(userId);
            // 获取用户 VO 对象
            UserVO userVO = userService.getUserVO(user);
            // 设置用户 VO
            appVO.setUser(userVO);
        }
        return appVO;
    }
}
