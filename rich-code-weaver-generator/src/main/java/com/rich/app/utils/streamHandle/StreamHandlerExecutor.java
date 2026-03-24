package com.rich.app.utils.streamHandle;

import com.rich.app.service.ChatHistoryService;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.model.enums.CodeGeneratorTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * AI 响应流处理器执行器
 *
 * @author DuRuiChi
 * @create 2025/12/27
 **/
@Component
public class StreamHandlerExecutor {
    @Resource
    private JsonStreamHandler jsonStreamHandler;

    @Resource
    private TestStreamHandler testStreamHandler;

    /**
     * 分类型对 AI 原始响应流进行处理
     *
     * @param stringFlux            AI 响应流
     * @param chatHistoryService    对话历史服务
     * @param appId                 产物 ID
     * @param userId                用户 ID
     * @param codeGeneratorTypeEnum 代码生成器类型枚举
     * @return reactor.core.publisher.Flux<org.springframework.http.codec.ServerSentEvent < java.lang.String>>  处理后响应给前端的 AI 响应流
     **/
    public Flux<ServerSentEvent<String>> executeStreamHandler(Flux<String> stringFlux, ChatHistoryService chatHistoryService, Long appId, Long userId, CodeGeneratorTypeEnum codeGeneratorTypeEnum) {
        // 根据代码生成类型选择对应的流处理策略
        return switch (codeGeneratorTypeEnum) {
            // Vue 项目模式：使用 JSON 流处理器
            // 推理模型输出的思考步骤、工具调用等信息都是 JSON 格式
            case VUE_PROJECT -> jsonStreamHandler.handleStream(stringFlux, chatHistoryService, appId, userId);
            // HTML 单文件模式、多文件模式：使用普通文本流处理器
            // 用于处理纯文本流式输出（不含 JSON 结构）
            case HTML, MULTI_FILE -> testStreamHandler.handleStream(stringFlux, chatHistoryService, appId, userId);
            // 默认情况：不支持的类型，抛出异常
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                    "不支持的代码生成器类型: " + codeGeneratorTypeEnum.getValue());
        };
    }
}
