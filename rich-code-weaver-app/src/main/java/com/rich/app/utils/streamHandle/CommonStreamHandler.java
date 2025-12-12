package com.rich.app.utils.streamHandle;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.rich.app.service.ChatHistoryService;
import com.rich.model.enums.ChatHistoryTypeEnum;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 通用流处理器，含 AI 响应流处理的通用逻辑
 *
 * @author DuRuiChi
 * @create 2025/8/27
 **/
@Component
public class CommonStreamHandler {
    /**
     * 处理 AI 响应流的通用逻辑，用于处理为响应给前端的最终内容
     *
     * @param stringFlux         AI 响应流
     * @param chatHistoryService 对话历史服务
     * @param appId              应用 ID
     * @param userId             用户 ID
     * @return 处理后的 AI 响应流
     */
    public Flux<ServerSentEvent<String>> handleStream(Flux<String> stringFlux, ChatHistoryService chatHistoryService, Long appId, Long userId, StringBuilder aiResponseBuilder) {
        // 处理 AI 响应流
        return stringFlux
                // 错误处理
                .doOnError(error -> {
                    chatHistoryService.addChatMessage(appId, "AI 响应失败,请联系管理员：" + error.getMessage(),
                            ChatHistoryTypeEnum.AI.getValue(),
                            userId);
                })
                // 封装为和前端约定好的 JOSN 格式的 SSE 事件
                .map(
                        strBlock -> {
                            // 封装为 JSON 字符串，预防直接进行字符串流式传输丢失空格符、换行符等问题
                            // {"b": "代码内容"} 格式，用于前端解析
                            // 注意： 前端应当具备对当前输出格式的解析能力
                            String jsonStrBlock = JSONUtil.toJsonStr(Map.of("b", strBlock));
                            // 封装为 SSE 事件
                            return ServerSentEvent.<String>builder()
                                    .data(jsonStrBlock)
                                    .build();
                        }
                )
                .onErrorResume(error -> {
                    String msg = error != null ? error.getMessage() : null;
                    boolean connectionAborted = (error instanceof java.io.IOException) && msg != null && (
                            msg.contains("软件中止了一个已建立的连接") ||
                                    msg.contains("Broken pipe") ||
                                    msg.contains("Connection reset"));
                    if (connectionAborted) {
                        return Flux.empty();
                    }
                    return Flux.just(
                            ServerSentEvent.<String>builder()
                                    .event("error")
                                    .data(JSONUtil.toJsonStr(Map.of("error", msg)))
                                    .build()
                    );
                })
                // 拼接结束事件
                // Flux 适用于处理 0-N 个项目的情况，而 Mono 适用于处理 0-1 个项目的情况，故使用 Mono.just() 执行一次结束事件拼接
                .concatWith(Mono.just(
                        ServerSentEvent.<String>builder()
                                .event("end")
                                .data("")
                                .build()
                ))
                .doFinally(signalType -> {
                    try {
                        String aiResponse = aiResponseBuilder.toString();
                        if (StrUtil.isNotBlank(aiResponse)) {
                            chatHistoryService.addChatMessage(appId,
                                    aiResponse,
                                    ChatHistoryTypeEnum.AI.getValue(),
                                    userId);
                        }
                    } catch (Exception e) {
                        chatHistoryService.addChatMessage(appId,
                                "AI 响应保存失败：" + e.getMessage(),
                                ChatHistoryTypeEnum.AI.getValue(),
                                userId);
                    }
                });
    }
}
