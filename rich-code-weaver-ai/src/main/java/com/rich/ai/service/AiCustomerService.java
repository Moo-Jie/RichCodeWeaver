package com.rich.ai.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AI 客服服务接口
 *
 * @author DuRuiChi
 * @create 2026/4/9
 */
public interface AiCustomerService {

    /**
     * AI 客服流式对话
     *
     * @param userMessage 用户消息
     * @param conversationId 会话 ID，作为对话记忆隔离键
     * @return TokenStream 流式响应
     */
    @UserMessage("{{userMessage}}")
    TokenStream chatStream(@V("userMessage") String userMessage, @MemoryId Long conversationId);
}
