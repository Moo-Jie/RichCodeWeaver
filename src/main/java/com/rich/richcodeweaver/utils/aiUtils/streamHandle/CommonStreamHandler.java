package com.rich.richcodeweaver.utils.aiUtils.streamHandle;

import com.rich.richcodeweaver.model.enums.ChatHistoryTypeEnum;
import com.rich.richcodeweaver.service.ChatHistoryService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

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
    public Flux<String> handleStream(Flux<String> stringFlux, ChatHistoryService chatHistoryService, Long appId, Long userId, StringBuilder aiResponseBuilder) {
        // WebSocket 模式下不再封装 ServerSentEvent；直接返回文本块。
        // 错误仍然需要记录到对话历史中，方便刷新后可追溯。
        return stringFlux.doOnError(error -> chatHistoryService.addChatMessage(
                appId,
                "AI 响应失败,请联系管理员：" + error.getMessage(),
                ChatHistoryTypeEnum.AI.getValue(),
                userId
        ));
    }
}
