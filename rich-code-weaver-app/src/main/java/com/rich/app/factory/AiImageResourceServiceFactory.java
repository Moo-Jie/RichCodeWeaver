package com.rich.app.factory;

import com.rich.ai.aiTools.ImageResource.AiGeneratorImageTool;
import com.rich.ai.aiTools.ImageResource.ImageSearchTool;
import com.rich.ai.guardrail.PromptSafetyInputGuardrail;
import com.rich.ai.service.AiImageResourceService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 图片收集 AI 服务实例工厂
 *
 * @author DuRuiChi
 * @create 2025/8/5
 **/
@Slf4j
@Configuration
public class AiImageResourceServiceFactory {

    /**
     * 多例模式下的图片收集 AI 服务模型
     **/
    @Resource(name = "imageResourceChatModel")
    private ChatModel imageResourceChatModel;

    /**
     * 图片收集 AI 服务
     */
    @Resource
    private ImageSearchTool imageSearchTool;

    /**
     * 图片生成 AI 服务
     */
    @Resource
    private AiGeneratorImageTool aiGeneratorImageTool;

    /**
     * 创建图片收集 AI 服务实例
     */
    @Bean
    public AiImageResourceService createAiImageGeneratorService() {
        return AiServices.builder(AiImageResourceService.class)
                .chatModel(imageResourceChatModel)
                // 配置提示词护轨规则
                .inputGuardrails(new PromptSafetyInputGuardrail())
                .tools(
                        imageSearchTool,
                        aiGeneratorImageTool
                )
                .build();
    }
}
