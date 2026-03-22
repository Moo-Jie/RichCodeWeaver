package com.rich.app.factory;

import com.rich.ai.service.AiCodeReviewService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 代码审查服务实例创建工厂
 * 负责创建用于代码审查的 AI 服务实例
 *
 * @author DuRuiChi
 * @since 2025-09-05
 */
@Slf4j
@Configuration
public class AiCodeReviewServiceFactory {

    /**
     * 多例模式下的代码审查 AI 服务模型
     **/
    @Resource(name = "codeReviewChatModel")
    private ChatModel codeReviewChatModel;

    /**
     * 创建 AI 代码审查服务实例
     * 使用专用的代码审查模型构建 AI 服务，用于对生成的代码进行质量审查
     *
     * @return AiCodeReviewService AI 代码审查服务实例
     * @author DuRuiChi
     */
    @Bean
    public AiCodeReviewService aiCodeReviewService() {
        return AiServices.builder(AiCodeReviewService.class)
                .chatModel(codeReviewChatModel)
                // TODO 配置提示词护轨规则，目前规则不够完善，容易导致误判
//                .inputGuardrails(new PromptSafetyInputGuardrail())
                .build();
    }
}
