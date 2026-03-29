package com.rich.app.factory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rich.ai.agent.AiCodeGenAgentService;
import com.rich.ai.aiTools.ToolsManager;
import com.rich.ai.rag.RagContentRetrieverAugmentorFactory;
import com.rich.app.service.ChatHistoryService;
import com.rich.client.innerService.InnerSystemPromptService;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.common.utils.SpringContextUtil;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI 代码生成 Agent 服务实例工厂
 * 基于 Caffeine 缓存管理 Agent 服务实例，对话历史存入 Redis
 *
 * @author DuRuiChi
 * @create 2026/4/1
 **/
@Slf4j
@Configuration
public class AiCodeGenAgentServiceFactory {

    /**
     * Agent 系统提示词标识
     */
    private static final String AGENT_PROMPT_KEY = "agent-code-gen-system-prompt";

    /**
     * Caffeine 缓存，用于存储 Agent 服务实例（相同 appId 复用，避免重复加载历史）
     */
    private final Cache<Long, AiCodeGenAgentService> caffeineCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) ->
                    log.debug("【Agent 服务实例缓存】已移除，appId: {}, 原因: {}", key, cause))
            .build();

    /**
     * 供 Agent 调用的所有工具包
     */
    @Resource
    private ToolsManager toolsManager;

    /**
     * Redis 类型的 ChatMemory 存储
     */
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    /**
     * 对话历史服务，用于从数据库加载历史消息
     */
    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * 系统提示词内部服务（通过 Dubbo 远程调用 prompt 模块）
     */
    @DubboReference
    private InnerSystemPromptService innerSystemPromptService;

    /**
     * RAG 检索增强器工厂（可选注入，RAG 未启用时为 null）
     * 用于为 Agent 模式创建专用的 RAG 检索增强器
     */
    @Autowired(required = false)
    private RagContentRetrieverAugmentorFactory ragContentRetrieverAugmentorFactory;

    /**
     * 从缓存中获取或创建 Agent 服务实例
     *
     * @param appId 产物 ID
     * @return AiCodeGenAgentService Agent 服务实例
     */
    public AiCodeGenAgentService getAgentService(Long appId) {
        return caffeineCache.get(appId, (key) -> createAgentService(appId));
    }

    /**
     * 创建 Agent 服务实例
     * 从数据库加载系统提示词和对话历史，配置所有工具，构建推理流式服务
     *
     * @param appId 产物 ID
     * @return AiCodeGenAgentService Agent 服务实例
     */
    private AiCodeGenAgentService createAgentService(Long appId) {
        log.info("为 appId: {} 创建 Agent 服务实例", appId);

        // 步骤1：从数据库加载 Agent 系统提示词
        String systemPrompt = innerSystemPromptService.getPromptContentByKey(AGENT_PROMPT_KEY);
        if (systemPrompt == null || systemPrompt.isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "未找到 Agent 系统提示词，promptKey=" + AGENT_PROMPT_KEY + "，请在管理后台配置");
        }

        // 步骤2：创建对话记忆（基于 appId 隔离，Redis 存储）
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(50)
                .build();

        // 步骤3：从数据库加载历史对话到 Redis 记忆中
        Boolean loaded = chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 10);
        if (Boolean.FALSE.equals(loaded)) {
            chatMemory.add(UserMessage.from("历史记录加载失败，可能已经过期。"));
            log.warn("为 appId: {} 加载 Agent 历史记录失败", appId);
        }

        // 步骤4：从 Spring 容器获取推理流式模型（多例模式，避免并发问题）
        StreamingChatModel reasoningStreamingChatModel =
                SpringContextUtil.getBean("reasoningStreamingChatModel", StreamingChatModel.class);

        // 步骤5：构建 Agent 服务（注入所有工具 + 系统提示词 + 对话记忆 + RAG）
        AiServices<AiCodeGenAgentService> builder = AiServices.builder(AiCodeGenAgentService.class)
                .streamingChatModel(reasoningStreamingChatModel)
                .systemMessageProvider(memoryId -> systemPrompt)
                .tools(toolsManager.getAllTools())
                // Agent 模式允许更多轮工具调用，支持复杂项目的自主生成
                .maxSequentialToolsInvocations(40)
                .chatMemory(chatMemory)
                .chatMemoryProvider(id -> chatMemory)
                // 当 AI 幻觉调用不存在的工具时，返回错误提示（防止异常中断）
                .hallucinatedToolNameStrategy(toolExecutionRequest ->
                        ToolExecutionResultMessage.from(toolExecutionRequest,
                                "Error: there is no tool called " + toolExecutionRequest.name()));

        // 步骤6：如果 RAG 已启用，注入 Agent 专用的 RAG 检索增强器
        // Agent 模式只检索 codeGenType=AGENT 的知识库文档，与工作流模式完全隔离
        if (ragContentRetrieverAugmentorFactory != null) {
            RetrievalAugmentor agentRagAugmentor = ragContentRetrieverAugmentorFactory.createAgentRetrievalAugmentor();
            builder.retrievalAugmentor(agentRagAugmentor);
            log.info("为 appId: {} 的 Agent 服务注入 RAG 检索增强器（codeGenType=AGENT）", appId);
        } else {
            log.info("RAG 未启用，appId: {} 的 Agent 服务不使用 RAG 检索增强", appId);
        }

        return builder.build();
    }
}
