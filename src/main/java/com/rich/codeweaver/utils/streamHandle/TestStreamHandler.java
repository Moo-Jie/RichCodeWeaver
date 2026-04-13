package com.rich.codeweaver.utils.streamHandle;

import cn.hutool.core.util.StrUtil;
import com.rich.codeweaver.service.generator.ChatHistoryService;
import jakarta.annotation.Resource;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 用于 AI 普通文本输出的流处理器
 * (用于 HTML 单文件模式、多文件模式的 AI 响应流处理器，用于处理纯文本流式输出)
 *
 * @author DuRuiChi
 * @create 2025/12/27
 **/
@Component
public class TestStreamHandler {
    @Resource
    private CommonStreamHandler commonStreamHandler;

    /**
     * 处理 AI 普通文本输出的流
     *
     * @param stringFlux         AI 响应流
     * @param chatHistoryService 对话历史服务
     * @param appId              产物 ID
     * @param userId             用户 ID
     * @return 处理后的 AI 响应流
     */
    public Flux<ServerSentEvent<String>> handleStream(Flux<String> stringFlux, ChatHistoryService chatHistoryService, Long appId, Long userId) {
        // 用于收集 AI 响应内容的 StringBuilder
        StringBuilder aiResponseBuilder = new StringBuilder();
        // 处理 AI 响应流
        // 注意：不要在此处添加 doOnComplete 保存对话历史，CommonStreamHandler.doFinally 已统一处理保存逻辑
        return commonStreamHandler.handleStream(
                // 直接收集 AI 响应内容的数据块，用于保存到对话历史
                stringFlux.map(strBlock -> {
                            aiResponseBuilder.append(strBlock);
                            return strBlock;
                        })
                        // 过滤空字串
                        .filter(StrUtil::isNotEmpty),
                chatHistoryService,
                appId,
                userId,
                aiResponseBuilder);
    }
}
