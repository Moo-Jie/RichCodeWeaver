package com.rich.codeweaver.factory;


import com.rich.codeweaver.common.exception.BusinessException;
import com.rich.codeweaver.common.exception.ErrorCode;
import com.rich.codeweaver.service.generator.AiCodeGeneratorTypeStrategyService;
import com.rich.codeweaver.service.prompt.SystemPromptService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 代码生成策略选择服务实例创建工厂
 * 负责创建用于选择代码生成策略的 AI 服务实例
 *
 * @author DuRuiChi
 * @since 2025-09-05
 */
@Slf4j
@Configuration
public class AiCodeGeneratorTypeStrategyServiceFactory {

    /**
     * 多例模式下的代码生成策略选择 AI 服务模型
     **/
    @Resource(name = "codeGeneratorTypeStrategyChatModel")
    private ChatModel codeGeneratorTypeStrategyChatModel;

    @Resource
    private SystemPromptService systemPromptService;

    /**
     * 创建 AI 代码生成策略选择服务实例
     * 使用专用的策略选择模型构建 AI 服务，用于智能选择最合适的代码生成类型
     *
     * @return AiCodeGeneratorTypeStrategyService AI 代码生成策略选择服务实例
     * @author DuRuiChi
     */
    @Bean
    public AiCodeGeneratorTypeStrategyService aiCodeGeneratorTypeStrategyService() {
        String systemPromptContent = systemPromptService.getPromptContentByKey("code-generation-strategy-system-prompt");
        if (systemPromptContent == null || systemPromptContent.isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "未找到系统提示词，promptKey=code-generation-strategy-system-prompt，请在管理后台配置");
        }
        return AiServices.builder(AiCodeGeneratorTypeStrategyService.class)
                .chatModel(codeGeneratorTypeStrategyChatModel)
                .systemMessageProvider(memoryId -> systemPromptContent)
                // TODO 配置提示词护轨规则，目前规则不够完善，容易导致误判
//                .inputGuardrails(new PromptSafetyInputGuardrail())
                .build();
    }
}
