package com.rich.richcodeweaver.service.aiChatService;

import com.rich.richcodeweaver.model.enums.CodeGeneratorTypeEnum;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * AI 代码生成策略选择服务接口
 *
 * @author DuRuiChi
 * @create 2025/9/5
 **/
public interface AiCodeGeneratorTypeStrategyService {
    /**
     * AI 代码生成策略选择
     *
     * @param userMessage 用户提示词
     * @return AI 的代码生成策略
     */
    @SystemMessage(fromResource = "/aiPrompt/code-generation-strategy-system-prompt.txt")
    CodeGeneratorTypeEnum getCodeGenStrategy(@UserMessage String userMessage);
}
