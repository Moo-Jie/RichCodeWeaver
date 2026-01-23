package com.rich.richcodeweaver.service.codegen;

import com.rich.richcodeweaver.exception.BusinessException;
import com.rich.richcodeweaver.exception.ErrorCode;
import com.rich.richcodeweaver.exception.ThrowUtils;
import com.rich.richcodeweaver.langGraph.CodeGenWorkflowApp;
import com.rich.richcodeweaver.model.entity.App;
import com.rich.richcodeweaver.model.enums.ChatHistoryTypeEnum;
import com.rich.richcodeweaver.model.enums.CodeGeneratorTypeEnum;
import com.rich.richcodeweaver.service.AppService;
import com.rich.richcodeweaver.service.ChatHistoryService;
import com.rich.richcodeweaver.utils.aiUtils.AIGenerateCodeAndSaveToFileUtils;
import com.rich.richcodeweaver.utils.aiUtils.streamHandle.StreamHandlerExecutor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 代码生成流式业务编排（与传输协议无关：SSE/WS 都应复用这里）
 */
@Service
@Slf4j
public class CodeGenStreamService {

    @Resource
    private AppService appService;

    @Resource
    private AIGenerateCodeAndSaveToFileUtils aiGenerateCodeAndSaveToFileUtils;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private CodeGenWorkflowApp codeGenWorkflowApp;

    /**
     * 创建“用户可读”的流式输出（每个元素是一段可直接追加到聊天窗口的文本）
     */
    public Flux<String> createUserFacingStream(Long appId, Long userId, String message, boolean isAgent) {
        // 关键：使用 defer，避免在 WebSocket handler 线程里“同步 throw”导致会话被 1011 关闭
        return Flux.defer(() -> {
            ThrowUtils.throwIf(appId == null || userId == null || appId <= 0 || userId <= 0 || message == null, ErrorCode.PARAMS_ERROR);

            // 查询应用 + 权限校验
            App app = appService.getById(appId);
            ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
            ThrowUtils.throwIf(!app.getUserId().equals(userId), ErrorCode.FORBIDDEN_ERROR);

            // 解析生成类型
            CodeGeneratorTypeEnum type = CodeGeneratorTypeEnum.getEnumByValue(app.getCodeGenType());
            if (type == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码生成类型为空");
            }

            // 先落库用户消息（刷新可恢复上下文）
            boolean isSaveMsg = chatHistoryService.addChatMessage(appId, message, ChatHistoryTypeEnum.USER.getValue(), userId);
            ThrowUtils.throwIf(!isSaveMsg, ErrorCode.OPERATION_ERROR, "保存用户消息失败");

            if (isAgent) {
                // Agent 工作流输出本身就是“用户可读”的文本流
                return codeGenWorkflowApp.executeWorkflow(message, type, appId, chatHistoryService, userId);
            }

            // 非 Agent：AI 原始流 -> 处理为前端展示文本流（并在 handler 内完成 AI 消息落库）
            Flux<String> rawFlux = aiGenerateCodeAndSaveToFileUtils.aiGenerateAndSaveCodeStream(message, type, appId);
            return streamHandlerExecutor.executeStreamHandler(rawFlux, chatHistoryService, appId, userId, type);
        });
    }
}


