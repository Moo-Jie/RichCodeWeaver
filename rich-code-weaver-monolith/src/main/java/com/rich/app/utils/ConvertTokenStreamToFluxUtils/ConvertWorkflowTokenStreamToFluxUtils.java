package com.rich.app.utils.ConvertTokenStreamToFluxUtils;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rich.ai.model.msgResponse.StreamAiChatMsgResponse;
import com.rich.ai.model.msgResponse.StreamToolExecutedMsgResponse;
import com.rich.ai.model.msgResponse.StreamToolInvocMsgResponse;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;

/**
 * Workflow 模式 TokenStream → Flux<String> 转换器
 *
 * @author DuRuiChi
 * @create 2025/12/25
 **/
@Slf4j
@Component
public class ConvertWorkflowTokenStreamToFluxUtils {
    /**
     * TokenStream (LangChain4j) 转换为 Flux<String> (Reactor )
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    public Flux<String> convertTokenStreamToFlux(TokenStream tokenStream, Long appId) {
        // 创建 Flux 流，使用 sink 发射器处理 TokenStream 事件
        return Flux.create(sink -> {
            // 用于跟踪已见过的工具ID，只发射每个工具的首次调用请求
            // 跳过后续的参数碎片（含大量文件内容），避免前端流量过大
            Set<String> seenToolIds = new HashSet<>();

            // 转换的主要逻辑：在 tokenStream 事件处理的回调函数中，使用 sink.next() 发射器
            // 将 AI 输出的信息转换为自定义封装类，再转换为 JSON 格式，最后发射出去
            // 注：当前稳定版本并不支持对 Tool 调用信息的输出功能，故使用新版本代码
            // 参考：https://github.com/langchain4j/langchain4j/pull/3303
            tokenStream
                    // 处理 AI 的普通响应（文本与代码块内容）
                    .onPartialResponse((String partialResponse) -> {
                        // 将部分响应封装为 StreamAiChatMsgResponse 对象
                        StreamAiChatMsgResponse streamAiChatMsgResponse = new StreamAiChatMsgResponse(partialResponse);
                        // 转换为 JSON 并发射到流中
                        sink.next(JSONUtil.toJsonStr(streamAiChatMsgResponse));
                    })
                    // 处理工具调用请求（只发射每个工具ID的首次请求）
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        String toolId = toolExecutionRequest.id();
                        // 检查是否是首次见到该工具ID
                        if (toolId != null && !seenToolIds.contains(toolId)) {
                            // 标记为已见，避免重复发射
                            seenToolIds.add(toolId);
                            // 封装工具调用请求并发射
                            StreamToolInvocMsgResponse streamToolInvocMsgResponse = new StreamToolInvocMsgResponse(toolExecutionRequest);
                            sink.next(JSONUtil.toJsonStr(streamToolInvocMsgResponse));
                        }
                    })
                    // 处理工具执行完成事件（剥离大体积字段）
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        // 封装工具执行结果
                        StreamToolExecutedMsgResponse streamToolExecutedMsgResponse = new StreamToolExecutedMsgResponse(toolExecution);
                        // 从 arguments 中移除大体积字段（文件内容），只保留路径等摘要信息
                        try {
                            JSONObject argsJson = JSONUtil.parseObj(streamToolExecutedMsgResponse.getArguments());
                            // 移除文件内容字段，减少流量传输
                            argsJson.remove("content");      // 新文件内容
                            argsJson.remove("oldContent");   // 旧文件内容
                            argsJson.remove("newContent");   // 更新后的文件内容
                            streamToolExecutedMsgResponse.setArguments(argsJson.toString());
                        } catch (Exception e) {
                            // 如果解析失败，使用原始参数
                            log.debug("剥离工具参数内容字段失败，使用原始参数", e);
                        }
                        // 发射处理后的工具执行结果
                        sink.next(JSONUtil.toJsonStr(streamToolExecutedMsgResponse));
                    })
                    // 处理完成事件
                    .onCompleteResponse((ChatResponse response) -> {
                        log.info("TokenStream 输出完成，appId={}", appId);
                        sink.complete();
                    })
                    // 处理错误事件，流截断和网络异常都必须失败关闭
                    .onError((Throwable error) -> {
                        log.error("TokenStream 转换为 Flux 流时发生错误, appId={}", appId, error);
                        sink.error(error);
                    })
                    // 启动 TokenStream 处理
                    .start();
        });
    }
}
