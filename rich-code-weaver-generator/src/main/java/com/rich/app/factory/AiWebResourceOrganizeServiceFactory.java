package com.rich.app.factory;

import com.rich.ai.aiTools.webOperate.AiWebScrapingTool;
import com.rich.ai.aiTools.webOperate.AiWebSearchTool;
import com.rich.ai.service.AiWebResourceOrganizeService;
import com.rich.client.innerService.InnerSystemPromptService;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网络资源整理 AI 服务实例创建工厂
 * 负责创建用于网络资源搜索和抓取的 AI 服务实例
 *
 * @author DuRuiChi
 * @since 2025-08-05
 */
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
     * 系统提示词内部服务（通过 Dubbo 远程调用 prompt 模块）
     **/
    @DubboReference
    private InnerSystemPromptService innerSystemPromptService;

    /**
     * 创建网络资源整理 AI 服务实例
     * 集成网络搜索和网页抓取工具，用于为代码项目收集和整理网络资源
     *
     * @return AiWebResourceOrganizeService 网络资源整理 AI 服务实例
     * @author DuRuiChi
     */
    @Bean
    public AiWebResourceOrganizeService createAiWebResourceOrganizeService() {
        String systemPromptContent = innerSystemPromptService.getPromptContentByKey("web-resource-organize-system-prompt");
        if (systemPromptContent == null || systemPromptContent.isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "未找到系统提示词，promptKey=web-resource-organize-system-prompt，请在管理后台配置");
        }
        return AiServices.builder(AiWebResourceOrganizeService.class)
                .chatModel(webResourceOrganizeChatModel)
                .systemMessageProvider(memoryId -> systemPromptContent)
                // TODO 配置提示词护轨规则，目前规则不够完善，容易导致误判
//                .inputGuardrails(new PromptSafetyInputGuardrail())
                .tools(
                        aiWebScrapingTool,
                        aiWebSearchTool
                )
                .build();
    }
}
