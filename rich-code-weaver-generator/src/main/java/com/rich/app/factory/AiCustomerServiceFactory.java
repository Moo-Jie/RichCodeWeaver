package com.rich.app.factory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rich.ai.rag.RagContentRetrieverAugmentorFactory;
import com.rich.ai.service.AiCustomerService;
import com.rich.app.service.AiChatSummaryService;
import com.rich.app.service.CustomerServiceMessageService;
import com.rich.client.innerService.InnerSystemPromptService;
import com.rich.common.constant.AiServiceConstant;
import com.rich.common.constant.CustomerServiceConstant;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.common.utils.SpringContextUtil;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI 客服服务实例工厂
 * 基于 Caffeine 缓存管理客服服务实例，对话历史存入 Redis
 * <p>
 * 对话上下文记忆架构与 Agent/Workflow 工厂保持一致：
 * 1. TokenWindowChatMemory —— 基于 Token 估算的滑动窗口动态加载
 * 2. ChunkedRedisChatMemoryStore —— 历史工具调用消息折叠，节约上下文
 * 3. AiChatSummaryService —— 每隔 N 条对话进行增量摘要总结
 * <p>
 * 会话隔离方式：以 conversationId 为记忆标识（区别于 Agent/Workflow 的 appId）
 *
 * @author DuRuiChi
 * @create 2026/4/9
 */
@Slf4j
@Configuration
public class AiCustomerServiceFactory {

    /**
     * 客服 RAG 未启用日志模板
     */
    private static final String RAG_DISABLED_LOG_TEMPLATE = "AI 客服 RAG 未启用，conversationId: {} 跳过检索增强";

    /**
     * Caffeine 缓存，用于存储客服服务实例（相同 conversationId 复用，避免重复加载历史）
     */
    private final Cache<Long, AiCustomerService> caffeineCache = Caffeine.newBuilder()
            .maximumSize(AiServiceConstant.AI_SERVICE_CACHE_MAX_SIZE)
            .expireAfterWrite(Duration.ofMinutes(AiServiceConstant.AI_SERVICE_EXPIRE_AFTER_WRITE_MINUTES))
            .expireAfterAccess(Duration.ofMinutes(AiServiceConstant.AI_SERVICE_EXPIRE_AFTER_ACCESS_MINUTES))
            .removalListener((key, value, cause) ->
                    log.debug("【AI 客服服务实例缓存】已移除，conversationId: {}, 原因: {}", key, cause))
            .build();

    /**
     * 对话记忆存储（底层为 Redis + 分片存储 + 工具折叠 + 超长截断装饰器，防止 bigkey）
     */
    @Resource
    private ChatMemoryStore chatMemoryStore;

    /**
     * Token 计数器，用于按 Token 数维护动态滑动窗口
     */
    @Resource
    private TokenCountEstimator tokenCountEstimator;

    /**
     * 客服消息服务，用于从数据库加载历史消息
     */
    @Resource
    private CustomerServiceMessageService customerServiceMessageService;

    /**
     * 系统提示词内部服务（通过 Dubbo 远程调用 prompt 模块）
     */
    @DubboReference
    private InnerSystemPromptService innerSystemPromptService;

    /**
     * 对话增量摘要服务，用于获取近期对话摘要补充全局上下文
     */
    @Resource
    private AiChatSummaryService aiChatSummaryService;

    /**
     * RAG 知识库检索增强工厂（可选注入，RAG 未启用时为 null）
     * 为客服模式创建专用的 RAG 检索增强器
     */
    @Autowired(required = false)
    private RagContentRetrieverAugmentorFactory ragContentRetrieverAugmentorFactory;

    /**
     * 从缓存中获取或创建客服服务实例
     *
     * @param conversationId 会话 ID
     * @return AiCustomerService 客服服务实例
     */
    public AiCustomerService getCustomerService(Long conversationId) {
        return caffeineCache.get(conversationId, key -> createCustomerService(conversationId));
    }

    /**
     * 判断 AI 客服是否启用了 RAG 检索增强
     *
     * @return 是否启用
     */
    public boolean isCustomerServiceRagEnabled() {
        return ragContentRetrieverAugmentorFactory != null;
    }

    /**
     * 创建客服服务实例
     * 从数据库加载系统提示词和对话历史，配置流式模型，构建 AI 服务
     *
     * @param conversationId 会话 ID
     * @return AiCustomerService 客服服务实例
     */
    private AiCustomerService createCustomerService(Long conversationId) {
        log.info("为 conversationId: {} 创建客服服务实例", conversationId);

        // 1.从数据库加载客服系统提示词
        String systemPrompt = loadCustomerServicePrompt();

        // 2.创建对话记忆（基于 conversationId 隔离，Redis 存储）
        TokenWindowChatMemory chatMemory = buildChatMemory(conversationId);

        // 3.从数据库加载历史对话到 Redis 记忆中
        loadCustomerServiceChatHistory(conversationId, chatMemory);

        // 4.从 Spring 容器获取客服流式模型（多例模式，避免并发问题）
        StreamingChatModel streamingChatModel =
                SpringContextUtil.getBean(AiServiceConstant.CUSTOMER_SERVICE_STREAMING_CHAT_MODEL_BEAN, StreamingChatModel.class);

        // 5.构建客服服务（注入系统提示词 + 对话记忆 + 摘要上下文 + RAG）
        AiServices<AiCustomerService> builder = buildBaseCustomerService(systemPrompt, chatMemory, streamingChatModel);

        // 6.如果 RAG 已启用，注入客服专用的 RAG 检索增强器
        attachCustomerServiceRagAugmentor(builder, conversationId);

        return builder.build();
    }

    /**
     * 加载客服系统提示词
     *
     * @return 系统提示词内容
     */
    private String loadCustomerServicePrompt() {
        String systemPrompt = innerSystemPromptService.getPromptContentByKey(CustomerServiceConstant.CUSTOMER_SERVICE_PROMPT_KEY);
        if (systemPrompt == null || systemPrompt.isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "未找到 AI 客服系统提示词，promptKey=" + CustomerServiceConstant.CUSTOMER_SERVICE_PROMPT_KEY + "，请在管理后台配置");
        }
        return systemPrompt;
    }

    /**
     * 构建客服对话记忆（Token 滑动窗口）
     * 底层 ChatMemoryStore 已集成工具消息折叠（foldToolMessages），无需额外配置
     *
     * @param conversationId 会话 ID（客服模式的记忆标识，区别于 Agent/Workflow 的 appId）
     * @return 对话记忆
     */
    private TokenWindowChatMemory buildChatMemory(Long conversationId) {
        log.info("构建 CustomerService TokenWindowChatMemory: conversationId={}, maxTokens={}",
                conversationId, AiServiceConstant.CHAT_MEMORY_MAX_TOKENS);
        return TokenWindowChatMemory.builder()
                .id(conversationId)
                // 两大核心参数：
                // 基于 Redis 的对话记忆存储模型（Big Key 切片/工具折叠/超长截断处理）
                .chatMemoryStore(chatMemoryStore)
                // 基于 OpenAiTokenCountEstimator 的 Token 估算器
                .maxTokens(AiServiceConstant.CHAT_MEMORY_MAX_TOKENS, tokenCountEstimator)
                .build();
    }

    /**
     * 加载客服历史消息到记忆中
     *
     * @param conversationId 会话 ID
     * @param chatMemory     对话记忆
     */
    private void loadCustomerServiceChatHistory(Long conversationId, ChatMemory chatMemory) {
        Boolean loaded = customerServiceMessageService.loadChatHistoryToMemory(
                conversationId, chatMemory, AiServiceConstant.CUSTOMER_SERVICE_HISTORY_LOAD_LIMIT);
        if (Boolean.FALSE.equals(loaded)) {
            chatMemory.add(UserMessage.from(AiServiceConstant.HISTORY_LOAD_FAILED_MESSAGE));
            log.warn("为 conversationId: {} 加载客服历史记录失败", conversationId);
        }
    }

    /**
     * 构建客服基础 AI 服务配置
     *
     * @param systemPrompt       系统提示词
     * @param chatMemory         对话记忆
     * @param streamingChatModel 流式模型
     * @return AI 服务构建器
     */
    private AiServices<AiCustomerService> buildBaseCustomerService(String systemPrompt,
                                                                   ChatMemory chatMemory,
                                                                   StreamingChatModel streamingChatModel) {
        return AiServices.builder(AiCustomerService.class)
                // 客服使用专用流式模型
                .streamingChatModel(streamingChatModel)
                // 系统提示词 + 增量摘要上下文拼接（systemMessageProvider 每次调用时动态获取最新摘要）
                .systemMessageProvider(memoryId -> appendSummaryToPrompt(systemPrompt, (Long) memoryId, AiServiceConstant.SUMMARY_TYPE_CUSTOMER_SERVICE))
                // 直接绑定当前 conversationId 对应的 chatMemory，保持多轮客服上下文连续
                .chatMemory(chatMemory)
                // 同时保留 chatMemoryProvider，兼容依赖 MemoryId 的调用方式
                .chatMemoryProvider(id -> chatMemory);
    }

    /**
     * 将近期对话摘要拼接到系统提示词末尾，为 LLM 提供全局上下文
     *
     * @param basePrompt  原始系统提示词
     * @param memoryId    记忆标识（conversationId）
     * @param summaryType 摘要类型
     * @return 拼接摘要后的系统提示词
     */
    private String appendSummaryToPrompt(String basePrompt, Long memoryId, String summaryType) {
        String summary = aiChatSummaryService.getLatestSummaryContent(memoryId, summaryType);
        if (summary == null || summary.isBlank()) {
            return basePrompt;
        }
        return basePrompt + AiServiceConstant.SUMMARY_CONTEXT_PREFIX + summary;
    }

    /**
     * 为客服服务挂载 RAG 检索增强器
     *
     * @param builder        AI 服务构建器
     * @param conversationId 会话 ID
     */
    private void attachCustomerServiceRagAugmentor(AiServices<AiCustomerService> builder, Long conversationId) {
        if (ragContentRetrieverAugmentorFactory == null) {
            log.debug(RAG_DISABLED_LOG_TEMPLATE, conversationId);
            return;
        }
        RetrievalAugmentor retrievalAugmentor = ragContentRetrieverAugmentorFactory
                .createCustomerServiceRetrievalAugmentor();
        builder.retrievalAugmentor(retrievalAugmentor);
        log.info("为 conversationId: {} 的客服服务注入 RAG 检索增强器", conversationId);
    }
}
