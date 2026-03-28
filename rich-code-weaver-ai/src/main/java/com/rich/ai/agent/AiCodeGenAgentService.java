package com.rich.ai.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AI 代码生成 Agent 服务接口
 * 基于 Langchain4j AiServices 实现，支持工具调用循环（ReAct 模式）
 * 系统提示词通过工厂类 AiCodeGenAgentServiceFactory 编程式指定（从数据库查询）
 *
 * @author DuRuiChi
 * @create 2026/4/1
 **/
public interface AiCodeGenAgentService {

    /**
     * Agent 流式对话（支持工具调用循环）
     * Langchain4j 内部自动处理 ReAct 循环：AI 响应 → 工具调用 → 结果回填 → 继续响应
     *
     * @param userMessage 用户消息
     * @param appId       产物 ID（用作 MemoryId，标识独立的对话记忆）
     * @return TokenStream 流式响应（包含文本块、工具调用请求、工具执行结果）
     */
    @UserMessage("{{userMessage}}")
    TokenStream chatStream(@V("userMessage") String userMessage, @MemoryId Long appId);
}
