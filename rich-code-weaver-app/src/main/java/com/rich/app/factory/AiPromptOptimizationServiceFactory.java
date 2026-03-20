package com.rich.app.factory;

import com.rich.ai.service.AiPromptOptimizationService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 提示词优化服务实例创建工厂
 *
 * @author DuRuiChi
 * @return
 * @create 2025/3/20
 **/
@Slf4j
@Configuration
public class AiPromptOptimizationServiceFactory {

    /**
     * 多例模式下的提示词优化 AI 服务模型
     **/
    @Resource(name = "promptOptimizationChatModel")
    private ChatModel promptOptimizationChatModel;

    /**
     * AI 提示词优化服务实例
     *
     * @return AI 提示词优化服务实例
     */
    @Bean
    public AiPromptOptimizationService aiPromptOptimizationService() {
        return AiServices.builder(AiPromptOptimizationService.class)
                .chatModel(promptOptimizationChatModel)
                // 配置提示词护轨规则
//                .inputGuardrails(new PromptSafetyInputGuardrail())
                .build();
    }
}
