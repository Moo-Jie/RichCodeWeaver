package com.rich.app.utils.ConvertTokenStreamToFluxUtils;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rich.ai.model.msgResponse.StreamAiChatMsgResponse;
import com.rich.ai.model.msgResponse.StreamToolExecutedMsgResponse;
import com.rich.ai.model.msgResponse.StreamToolInvocMsgResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Agent 模式 TokenStream → Flux<String> 转换器
 *
 *
 * @author DuRuiChi
 * @create 2026/4/1
 **/
@Slf4j
@Component
public class ConvertAgentTokenStreamToFluxUtils {

    /**
     * 循环检测：滑动窗口大小
     * 增大窗口以减少正常连续文件操作时的误报
     */
    private static final int STUCK_WINDOW_SIZE = 10;

    /**
     * 循环检测：触发阈值（同一工具在窗口内出现次数 >= 此值则判定为循环）
     * 提高阈值，因为连续创建多个文件是正常行为
     */
    private static final int STUCK_DUPLICATE_THRESHOLD = 8;
    
    /**
     * 不参与循环检测的工具列表（这些工具连续调用是正常行为）
     */
    private static final Set<String> EXCLUDED_TOOLS = Set.of(
            "creatAndWrite",  // 连续创建多个文件是正常的
            "taskPlan",       // 连续更新任务状态是正常的
            "readFile",       // 连续读取多个文件进行自检是正常的
            "readDir",        // 连续读取多个目录是正常的
            "modifyFile",     // 连续修改多个文件是正常的
            "think",          // 连续思考是正常的
            "sendMessage"     // 连续发送消息是正常的
    );

    /**
     * 每个 appId 对应的最近工具调用名称滑动窗口
     * key: appId, value: 最近 STUCK_WINDOW_SIZE 次工具调用的名称队列
     */
    private final Map<Long, Deque<String>> recentToolCallsMap = new ConcurrentHashMap<>();

    /**
     * Agent 模式 TokenStream → Flux<String>
     * 包含工具调用循环检测。
     *
     * @param tokenStream TokenStream 对象
     * @param appId       产物 ID，用于循环检测的状态隔离
     * @return Flux<String> 流式响应（JSON 格式消息块）
     */
    public Flux<String> convertTokenStreamToFlux(TokenStream tokenStream, Long appId) {
        return convertTokenStreamToFlux(tokenStream, appId, null);
    }

    /**
     * Agent 模式 TokenStream → Flux<String>（含 Act 回调）
     *
     * @param tokenStream   TokenStream 对象
     * @param appId         产物 ID
     * @param actCallback   每次工具执行完成后的 Observation 回调（对应 ReActAgent.act()），可为 null
     * @return Flux<String> 流式响应
     */
    public Flux<String> convertTokenStreamToFlux(TokenStream tokenStream, Long appId,
                                                  BiConsumer<ToolExecution, Long> actCallback) {
        return Flux.create(sink -> {
            Set<String> seenToolIds = new HashSet<>();

            tokenStream
                    // AI 普通文本响应
                    .onPartialResponse((String partialResponse) -> {
                        StreamAiChatMsgResponse response = new StreamAiChatMsgResponse(partialResponse);
                        sink.next(JSONUtil.toJsonStr(response));
                    })
                    // 工具调用请求（只发射每个工具 ID 的首次请求，避免参数碎片刷屏）
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        String toolId = toolExecutionRequest.id();
                        if (toolId != null && !seenToolIds.contains(toolId)) {
                            seenToolIds.add(toolId);
                            StreamToolInvocMsgResponse invocResponse = new StreamToolInvocMsgResponse(toolExecutionRequest);
                            sink.next(JSONUtil.toJsonStr(invocResponse));
                        }
                    })
                    // 工具执行完成
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        StreamToolExecutedMsgResponse executedResponse = new StreamToolExecutedMsgResponse(toolExecution);

                        // 剥离大体积字段（文件内容），只保留路径等摘要信息
                        try {
                            JSONObject argsJson = JSONUtil.parseObj(executedResponse.getArguments());
                            argsJson.remove("content");
                            argsJson.remove("oldContent");
                            argsJson.remove("newContent");
                            executedResponse.setArguments(argsJson.toString());
                        } catch (Exception e) {
                            log.debug("剥离工具参数内容字段失败，使用原始参数", e);
                        }
                        sink.next(JSONUtil.toJsonStr(executedResponse));

                        // Act 回调（Observation）：通知 ReActAgent 子类工具执行完成
                        if (actCallback != null) {
                            actCallback.accept(toolExecution, appId);
                        }
                        // 循环检测：记录本次工具调用名称
                        String toolName = toolExecution.request().name();
                        checkAndHandleStuckState(appId, toolName, sink);
                    })
                    // Agent 完成（AI 自主决定停止，调用 exit 工具或达到 maxSequentialToolsInvocations）
                    // 注意：不触发构建，AI 已通过 buildProject 工具完成构建
                    .onCompleteResponse((response) -> {
                        log.info("Agent TokenStream 输出完成，appId={}", appId);
                        recentToolCallsMap.remove(appId);
                        sink.complete();
                    })
                    // 错误处理
                    .onError((Throwable error) -> {
                        String errorMsg = error.getMessage() != null ? error.getMessage() : "";
                        // JsonEOFException：AI 响应被截断，但工具调用（文件写入）可能已成功
                        if (errorMsg.contains("JsonEOFException") || errorMsg.contains("Unexpected end-of-input")) {
                            log.warn("Agent TokenStream JSON 截断异常，优雅完成: {}", errorMsg);
                            recentToolCallsMap.remove(appId);
                            sink.complete();
                        } else {
                            log.error("Agent TokenStream 错误, appId={}", appId, error);
                            recentToolCallsMap.remove(appId);
                            sink.error(error);
                        }
                    })
                    .start();
        });
    }

    /**
     * 循环检测与处理
     * 若同一工具名在滑动窗口内重复次数 >= 阈值，则向流中注入警告，提示 AI 更换策略
     * 
     * 优化：排除正常连续调用的工具（如文件创建、任务更新等），避免误报
     *
     * @param appId    产物 ID
     * @param toolName 本次调用的工具名称
     * @param sink     流发射器
     */
    private void checkAndHandleStuckState(Long appId, String toolName,
                                          reactor.core.publisher.FluxSink<String> sink) {
        // 排除列表中的工具不参与循环检测
        if (EXCLUDED_TOOLS.contains(toolName)) {
            return;
        }
        
        Deque<String> window = recentToolCallsMap.computeIfAbsent(appId, k -> new ArrayDeque<>());

        // 维护固定大小滑动窗口
        window.addLast(toolName);
        if (window.size() > STUCK_WINDOW_SIZE) {
            window.removeFirst();
        }

        // 计算当前工具在窗口内的出现次数
        long duplicateCount = window.stream().filter(name -> name.equals(toolName)).count();

        if (duplicateCount >= STUCK_DUPLICATE_THRESHOLD) {
            String stuckWarning = "\n\n> **[系统检测]** 检测到工具 `" + toolName + "` 在最近 " + STUCK_WINDOW_SIZE
                    + " 次调用中重复出现 " + duplicateCount + " 次，可能陷入循环。\n"
                    + "> 请重新评估当前策略：考虑简化实现、跳过失败步骤，或调用 `exit` 说明原因。\n\n";

            StreamAiChatMsgResponse warning = new StreamAiChatMsgResponse(stuckWarning);
            sink.next(JSONUtil.toJsonStr(warning));
            log.warn("Agent 循环检测触发: appId={}, toolName={}, 窗口内重复次数={}", appId, toolName, duplicateCount);

            // 窗口清空，给 AI 一个新的机会
            window.clear();
        }
    }
}
