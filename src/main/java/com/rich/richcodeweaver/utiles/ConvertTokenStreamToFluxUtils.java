package com.rich.richcodeweaver.utiles;

import cn.hutool.json.JSONUtil;
import com.rich.richcodeweaver.model.aiChatResponse.msgResponse.StreamAiChatMsgResponse;
import com.rich.richcodeweaver.model.aiChatResponse.msgResponse.StreamToolExecutedMsgResponse;
import com.rich.richcodeweaver.model.aiChatResponse.msgResponse.StreamToolInvocMsgResponse;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import reactor.core.publisher.Flux;

/**
 * 将 LangChain4j 的 TokenStream 转换为 Reactor 的 Flux<String> 工具类
 * （用于推理模型的流式输出，因为推理模型的流式输出是一个个 Token 逐个输出 ，而不是一次输出完，此处处理为通用 Flux 流）
 *
 * @author DuRuiChi
 * @create 2025/8/25
 **/
public class ConvertTokenStreamToFluxUtils {

    /**
     * TokenStream (LangChain4j) 转换为 Flux<String> (Reactor )
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> convertTokenStreamToFlux(TokenStream tokenStream) {
        // 创建 Flux 流，使用 sink 发射器处理 TokenStream 事件
        return Flux.create(sink -> {
            // 转换的主要逻辑 ：在 tokenStream 事件处理的回调函数中，使用 sink.next() 发射器，把 AI 输出的信息转 ——> 自定义封装类 ——> JSON 格式，最后发射出去
            // 注：在事件处理的回调函数中，当前稳定版本并不支持对 Tool 调用信息的输出功能，故直接拿新版本的代码进行了覆盖（https://github.com/langchain4j/langchain4j/pull/3303）
            tokenStream
                    // 处理 AI 的普通响应（即文本与代码块内容）
                    .onPartialResponse((String partialResponse) -> {
                        StreamAiChatMsgResponse streamAiChatMsgResponse = new StreamAiChatMsgResponse(partialResponse);
                        sink.next(JSONUtil.toJsonStr(streamAiChatMsgResponse));
                    })
                    // 处理工具调用请求
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        StreamToolInvocMsgResponse streamToolInvocMsgResponse = new StreamToolInvocMsgResponse(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(streamToolInvocMsgResponse));
                    })
                    // 处理工具执行完成事件
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        StreamToolExecutedMsgResponse streamToolExecutedMsgResponse = new StreamToolExecutedMsgResponse(toolExecution);
                        sink.next(JSONUtil.toJsonStr(streamToolExecutedMsgResponse));
                    })
                    // 处理完成事件
                    .onCompleteResponse((ChatResponse response) -> {
                        sink.complete();
                    })
                    // 处理错误事件
                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        sink.error(error);
                    })
                    // 启动 TokenStream
                    .start();
        });
    }
}