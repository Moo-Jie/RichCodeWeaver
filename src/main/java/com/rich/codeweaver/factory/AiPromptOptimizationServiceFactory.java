package com.rich.codeweaver.factory;


import com.rich.codeweaver.common.exception.BusinessException;
import com.rich.codeweaver.common.exception.ErrorCode;
import com.rich.codeweaver.service.prompt.AiPromptOptimizationService;
import com.rich.codeweaver.service.prompt.SystemPromptService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 提示词优化服务实例创建工厂
 * 负责创建用于优化用户输入提示词的 AI 服务实例
 *
 * @author DuRuiChi
 * @since 2025-03-20
 */
@Slf4j
@Configuration
public class AiPromptOptimizationServiceFactory {

    /**
     * 多例模式下的提示词优化 AI 服务模型
     **/
    @Resource(name = "promptOptimizationChatModel")
    private ChatModel promptOptimizationChatModel;

    /**
     * 系统提示词内部服务
     **/
    @Resource
    private SystemPromptService systemPromptService;

    /**
     * 创建 AI 提示词优化服务实例
     * 使用专用的提示词优化模型，将用户的简单描述优化为更详细、更准确的代码生成提示词
     *
     * @return AiPromptOptimizationService AI 提示词优化服务实例
     * @author DuRuiChi
     */
    @Bean
    public AiPromptOptimizationService aiPromptOptimizationService() {
        String systemPromptContent = systemPromptService.getPromptContentByKey("prompt-optimization-system-prompt");
        if (systemPromptContent == null || systemPromptContent.isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "未找到系统提示词，promptKey=prompt-optimization-system-prompt，请在管理后台配置");
        }
        // 构建 AI 提示词优化服务实例
        return AiServices.builder(AiPromptOptimizationService.class)
                // 配置专用的提示词优化 AI 模型
                .chatModel(promptOptimizationChatModel)
                .systemMessageProvider(memoryId -> systemPromptContent)
                // TODO 配置提示词护轨规则，目前规则不够完善，容易导致误判
//                .inputGuardrails(new PromptSafetyInputGuardrail())
                .build();
    }
}
