package com.rich.app.factory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rich.ai.agent.AiCodeGenAgentService;
import com.rich.ai.aiTools.ToolsManager;
import com.rich.ai.rag.RagContentRetrieverAugmentorFactory;
import com.rich.app.service.ChatHistoryService;
import com.rich.client.innerService.InnerSystemPromptService;
import com.rich.common.constant.AiServiceConstant;
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
import dev.langchain4j.service.tool.ToolProvider;
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
     * RAG 未启用日志模板
     */
    private static final String RAG_DISABLED_LOG_TEMPLATE = "RAG 未启用，appId: {} 的 Agent 服务不使用 RAG 检索增强";

    /**
     * Caffeine 缓存，用于存储 Agent 服务实例（相同 appId 复用，避免重复加载历史）
     */
    private final Cache<Long, AiCodeGenAgentService> caffeineCache = Caffeine.newBuilder()
            .maximumSize(AiServiceConstant.AI_SERVICE_CACHE_MAX_SIZE)
            .expireAfterWrite(Duration.ofMinutes(AiServiceConstant.AI_SERVICE_EXPIRE_AFTER_WRITE_MINUTES))
            .expireAfterAccess(Duration.ofMinutes(AiServiceConstant.AI_SERVICE_EXPIRE_AFTER_ACCESS_MINUTES))
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
     * MCP 工具提供器（可选注入）
     * 启用 MCP 后，Agent 模式可在原有本地工具之外调用外部 MCP 工具
     */
    @Autowired(required = false)
    private ToolProvider mcpToolProvider;

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

        // 1.从数据库加载 Agent 系统提示词
        String systemPrompt = loadAgentSystemPrompt();

        // 2.创建对话记忆（基于 appId 隔离，Redis 存储）
        MessageWindowChatMemory chatMemory = buildChatMemory(appId);

        // 3.从数据库加载历史对话到 Redis 记忆中
        loadAgentChatHistory(appId, chatMemory);

        // 4.从 Spring 容器获取推理流式模型（多例模式，避免并发问题）
        StreamingChatModel reasoningStreamingChatModel =
                SpringContextUtil.getBean(AiServiceConstant.REASONING_STREAMING_CHAT_MODEL_BEAN, StreamingChatModel.class);

        // 5.构建 Agent 服务（注入所有工具 + 系统提示词 + 对话记忆 + RAG）
        AiServices<AiCodeGenAgentService> builder = buildBaseAgentService(systemPrompt, chatMemory, reasoningStreamingChatModel);

        // 6.如果 RAG 已启用，注入 Agent 专用的 RAG 检索增强器
        // Agent 模式只检索 codeGenType=AGENT 的知识库文档，与工作流模式完全隔离
        attachAgentRagAugmentor(builder, appId);

        return builder.build();
    }

    /**
     * 加载 Agent 系统提示词
     *
     * @return 系统提示词内容
     */
    private String loadAgentSystemPrompt() {
        String systemPrompt = innerSystemPromptService.getPromptContentByKey(AiServiceConstant.AGENT_SYSTEM_PROMPT_KEY);
        if (systemPrompt == null || systemPrompt.isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "未找到 Agent 系统提示词，promptKey=" + AiServiceConstant.AGENT_SYSTEM_PROMPT_KEY + "，请在管理后台配置");
        }
        return systemPrompt;
    }

    /**
     * 构建 Agent 对话记忆
     *
     * @param appId 产物 ID
     * @return 对话记忆
     */
    private MessageWindowChatMemory buildChatMemory(Long appId) {
        return MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(AiServiceConstant.CHAT_MEMORY_MAX_MESSAGES)
                .build();
    }

    /**
     * 加载 Agent 历史消息到记忆中
     *
     * @param appId 产物 ID
     * @param chatMemory 对话记忆
     */
    private void loadAgentChatHistory(Long appId, MessageWindowChatMemory chatMemory) {
        Boolean loaded = chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, AiServiceConstant.DEFAULT_HISTORY_LOAD_LIMIT);
        if (Boolean.FALSE.equals(loaded)) {
            chatMemory.add(UserMessage.from(AiServiceConstant.HISTORY_LOAD_FAILED_MESSAGE));
            log.warn("为 appId: {} 加载 Agent 历史记录失败", appId);
        }
    }

    /**
     * 构建 Agent 基础服务配置
     *
     * @param systemPrompt 系统提示词
     * @param chatMemory 对话记忆
     * @param reasoningStreamingChatModel 推理模型
     * @return Agent 服务构建器
     */
    private AiServices<AiCodeGenAgentService> buildBaseAgentService(String systemPrompt,
                                                                    MessageWindowChatMemory chatMemory,
                                                                    StreamingChatModel reasoningStreamingChatModel) {
        // Agent 模式本身就是推理 + 工具调用链，因此这里直接将本地工具与 MCP 工具一并挂载
        AiServices<AiCodeGenAgentService> builder = AiServices.builder(AiCodeGenAgentService.class)
                // Agent 全程使用推理流式模型，确保 Think -> Act -> Observe 链路稳定运行
                .streamingChatModel(reasoningStreamingChatModel)
                // 系统提示词仍然沿用数据库中的 Agent Prompt，不改变原有规划/执行策略
                .systemMessageProvider(memoryId -> systemPrompt)
                // 原有本地工具全部保留，例如 taskPlan / think / 文件工具 等
                .tools(toolsManager.getAllTools())
                // 限制连续工具调用次数，避免模型异常循环调用工具
                .maxSequentialToolsInvocations(AiServiceConstant.AGENT_MAX_TOOLS_INVOCATIONS)
                // 直接绑定当前 appId 对应的 chatMemory，保持多轮 Agent 上下文连续
                .chatMemory(chatMemory)
                // 同时保留 chatMemoryProvider，兼容依赖 MemoryId 的调用方式
                .chatMemoryProvider(id -> chatMemory)
                // 当模型调用了不存在的工具名时，返回可读的失败信息，而不是直接抛异常中断
                .hallucinatedToolNameStrategy(toolExecutionRequest ->
                        ToolExecutionResultMessage.from(toolExecutionRequest,
                                String.format(AiServiceConstant.HALLUCINATED_TOOL_MESSAGE_TEMPLATE, toolExecutionRequest.name())));
        if (mcpToolProvider != null) {
            // 未启用 MCP 时不注入，保持原有 Agent 行为完全不变
            // 启用后，Agent 可以在本地工具之外额外调用 MCP 暴露出来的外部工具
            builder.toolProvider(mcpToolProvider);
        }

        // 返回 builder 继续给后面的 RAG 挂载逻辑复用，最后在 createAgentService 中统一 build
        return builder;
    }

    /**
     * 为 Agent 服务挂载 RAG 检索增强器
     *
     * @param builder Agent 服务构建器
     * @param appId 产物 ID
     */
    private void attachAgentRagAugmentor(AiServices<AiCodeGenAgentService> builder, Long appId) {
        if (ragContentRetrieverAugmentorFactory == null) {
            log.info(RAG_DISABLED_LOG_TEMPLATE, appId);
            return;
        }
        RetrievalAugmentor agentRagAugmentor = ragContentRetrieverAugmentorFactory.createAgentRetrievalAugmentor();
        builder.retrievalAugmentor(agentRagAugmentor);
        log.info("为 appId: {} 的 Agent 服务注入 RAG 检索增强器（codeGenType=AGENT）", appId);
    }
}
