package com.rich.app.factory;

import com.rich.ai.aiTools.ImageResource.AiGeneratorImageTool;
import com.rich.ai.aiTools.ImageResource.ImageSearchTool;
import com.rich.ai.service.AiImageResourceService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 图片资源 AI 服务实例创建工厂
 * 负责创建用于图片搜索和生成的 AI 服务实例
 *
 * @author DuRuiChi
 * @since 2025-08-05
 */
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
     * 创建图片资源 AI 服务实例
     * 集成图片搜索和 AI 图片生成工具，用于为代码项目提供图片资源
     *
     * @return AiImageResourceService 图片资源 AI 服务实例
     * @author DuRuiChi
     */
    @Bean
    public AiImageResourceService createAiImageGeneratorService() {
        return AiServices.builder(AiImageResourceService.class)
                .chatModel(imageResourceChatModel)
                // TODO 配置提示词护轨规则，目前规则不够完善，容易导致误判
//                .inputGuardrails(new PromptSafetyInputGuardrail())
                .tools(
                        imageSearchTool,
                        aiGeneratorImageTool
                )
                .build();
    }
}
