package com.rich.ai.service;

import dev.langchain4j.service.UserMessage;

/**
 * AI 提示词优化服务接口
 * 系统提示词通过 systemMessageProvider 在工厂类中编程式指定（从数据库查询）
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
    String optimizePrompt(@UserMessage String userPrompt);
}
