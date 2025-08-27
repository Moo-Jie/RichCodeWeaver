package com.rich.richcodeweaver.utiles.aiUtils.streamHandle;

import com.rich.richcodeweaver.exception.BusinessException;
import com.rich.richcodeweaver.exception.ErrorCode;
import com.rich.richcodeweaver.model.enums.CodeGeneratorTypeEnum;
import com.rich.richcodeweaver.service.ChatHistoryService;
import jakarta.annotation.Resource;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * AI 响应流处理器执行器
 *
 * @author DuRuiChi
 * @create 2025/8/27
 **/
@Component
public class StreamHandlerExecutor {
    @Resource
    private JsonStreamHandler jsonStreamHandler;

    @Resource
    private TestStreamHandler testStreamHandler;

    /**
     * 分类型对 AI 响应流进行处理
     *
     * @param stringFlux            AI 响应流
     * @param chatHistoryService    对话历史服务
     * @param appId                 应用 ID
     * @param userId                用户 ID
     * @param codeGeneratorTypeEnum 代码生成器类型枚举
     * @return reactor.core.publisher.Flux<org.springframework.http.codec.ServerSentEvent < java.lang.String>>
     **/
    public Flux<ServerSentEvent<String>> executeStreamHandler(Flux<String> stringFlux, ChatHistoryService chatHistoryService, Long appId, Long userId, CodeGeneratorTypeEnum codeGeneratorTypeEnum) {
        return switch (codeGeneratorTypeEnum) {
            // 处理 JSON 格式的 AI 响应流
            // (用于推理构建 Vue 项目模式的 AI 响应流处理器，因为推理模型关于 【思考步骤】 和 【工具调用】 等信息，是用 JOSN 格式输出的)
            case VUE_PROJECT -> jsonStreamHandler.handleStream(stringFlux, chatHistoryService, appId, userId);
            // 处理普通文本格式的 AI 响应流
            // (用于 HTML 单文件模式、多文件模式的 AI 响应流处理器，用于处理纯文本流式输出)
            case HTML, MULTI_FILE -> testStreamHandler.handleStream(stringFlux, chatHistoryService, appId, userId);
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的代码生成器类型");
        };
    }
}
