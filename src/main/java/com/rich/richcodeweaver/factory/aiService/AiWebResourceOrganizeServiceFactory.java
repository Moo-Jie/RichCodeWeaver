package com.rich.richcodeweaver.factory.aiService;

import com.rich.richcodeweaver.aiTools.webOperate.AiWebScrapingTool;
import com.rich.richcodeweaver.aiTools.webOperate.AiWebSearchTool;
import com.rich.richcodeweaver.service.aiChatService.AiWebResourceOrganizeService;
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
public class AiWebResourceOrganizeServiceFactory {

    /**
     * 多例模式下的网络资源整理 AI 服务模型
     **/
    @Resource(name = "webResourceOrganizeChatModel")
    private ChatModel webResourceOrganizeChatModel;

    /**
     * 网络资源抓取工具
     **/
    @Resource
    private AiWebScrapingTool aiWebScrapingTool;

    /**
     * 网络资源搜索工具
     **/
    @Resource
    private AiWebSearchTool aiWebSearchTool;

    /**
     * 创建网络资源整理 AI 服务实例
     */
    @Bean
    public AiWebResourceOrganizeService createAiWebResourceOrganizeService() {
        return AiServices.builder(AiWebResourceOrganizeService.class)
                .chatModel(webResourceOrganizeChatModel)
                .tools(
                        aiWebScrapingTool,
                        aiWebSearchTool
                )
                .build();
    }
}
