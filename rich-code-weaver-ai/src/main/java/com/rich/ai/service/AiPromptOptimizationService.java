package com.rich.ai.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * AI 提示词优化服务接口
 *
 * @author DuRuiChi
 * @create 2025/11/20
 **/
public interface AiPromptOptimizationService {
    /**
     * 优化用户提示词
     *
     * @param userPrompt 用户原始提示词
     * @return 优化后的提示词
     */
    @SystemMessage(fromResource = "/aiPrompt/prompt-optimization-system-prompt.txt")
    String optimizePrompt(@UserMessage String userPrompt);
}
