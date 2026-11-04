package com.rich.ai.service;

import com.rich.model.enums.CodeGeneratorTypeEnum;
import dev.langchain4j.service.UserMessage;

/**
 * AI 代码生成策略选择服务接口
 * 系统提示词通过 systemMessageProvider 在工厂类中编程式指定（从数据库查询）
 *
 * @author DuRuiChi
 * @create 2026/1/5
 **/
public interface AiCodeGeneratorTypeStrategyService {
    /**
     * AI 代码生成策略选择
     *
     * @param userMessage 用户提示词
     * @return AI 的代码生成策略
     */
    CodeGeneratorTypeEnum getCodeGenStrategy(@UserMessage String userMessage);
}
