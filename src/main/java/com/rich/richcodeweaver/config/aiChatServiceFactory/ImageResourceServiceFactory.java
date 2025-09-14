package com.rich.richcodeweaver.config.aiChatServiceFactory;

import com.rich.richcodeweaver.aiTools.ImageResource.AiGeneratorImageTool;
import com.rich.richcodeweaver.aiTools.ImageResource.ImageSearchTool;
import com.rich.richcodeweaver.service.aiChatService.AiImageResourceService;
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
public class ImageResourceServiceFactory {

    /**
     * 基础模型
     **/
    @Resource
    private ChatModel chatModel;

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
                .chatModel(chatModel)
                .tools(
                        imageSearchTool,
                        aiGeneratorImageTool
                )
                .build();
    }
}
