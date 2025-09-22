package com.rich.richcodeweaver.factory.aiService;

import com.rich.richcodeweaver.service.aiChatService.AiCodeReviewService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 代码审查服务实例创建工厂
 *
 * @author DuRuiChi
 * @return
 * @create 2025/9/5
 **/
@Slf4j
@Configuration
public class AiCodeReviewServiceFactory {

    /**
     * 多例模式下的代码审查 AI 服务模型
     **/
    @Resource(name = "codeReviewChatModel")
    private ChatModel codeReviewChatModel;

    /**
     * AI 代码审查服务实例
     *
     * @return AI 代码审查服务实例
     */
    @Bean
    public AiCodeReviewService aiCodeReviewService() {
        return AiServices.builder(AiCodeReviewService.class)
                .chatModel(codeReviewChatModel)
                .build();
    }
}
