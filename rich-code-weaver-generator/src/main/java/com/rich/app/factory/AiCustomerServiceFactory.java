package com.rich.app.factory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rich.ai.rag.RagContentRetrieverAugmentorFactory;
import com.rich.ai.service.AiCustomerService;
import com.rich.common.constant.AiServiceConstant;
import com.rich.common.constant.CustomerServiceConstant;
import com.rich.app.service.CustomerServiceMessageService;
import com.rich.client.innerService.InnerSystemPromptService;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.common.utils.SpringContextUtil;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
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
 * AI 客服服务实例工厂
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
    private static final String CUSTOMER_SERVICE_RAG_DISABLED_LOG_TEMPLATE = "AI 客服 RAG 未启用，conversationId: {} 跳过检索增强";

    private final Cache<Long, AiCustomerService> caffeineCache = Caffeine.newBuilder()
            .maximumSize(AiServiceConstant.AI_SERVICE_CACHE_MAX_SIZE)
            .expireAfterWrite(Duration.ofMinutes(AiServiceConstant.AI_SERVICE_EXPIRE_AFTER_WRITE_MINUTES))
            .expireAfterAccess(Duration.ofMinutes(AiServiceConstant.AI_SERVICE_EXPIRE_AFTER_ACCESS_MINUTES))
            .removalListener((key, value, cause) ->
                    log.debug("【AI 客服服务实例缓存】已移除，conversationId: {}, 原因: {}", key, cause))
            .build();

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private CustomerServiceMessageService customerServiceMessageService;

    @DubboReference
    private InnerSystemPromptService innerSystemPromptService;

    @Autowired(required = false)
    private RagContentRetrieverAugmentorFactory ragContentRetrieverAugmentorFactory;

    /**
     * 获取 AI 客服服务实例
     * 相同会话会复用缓存中的实例
     *
     * @param conversationId 会话 ID
     * @return AI 客服服务实例
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
     * 创建 AI 客服服务实例
     *
     * @param conversationId 会话 ID
     * @return AI 客服服务实例
     */
    private AiCustomerService createCustomerService(Long conversationId) {
        String systemPrompt = loadCustomerServicePrompt();

        // 每个客服会话维护一份独立的记忆窗口
        MessageWindowChatMemory chatMemory = buildChatMemory(conversationId);

        // 先把历史消息恢复进记忆，避免上下文丢失
        loadCustomerServiceChatHistory(conversationId, chatMemory);

        StreamingChatModel streamingChatModel =
                SpringContextUtil.getBean(AiServiceConstant.CUSTOMER_SERVICE_STREAMING_CHAT_MODEL_BEAN, StreamingChatModel.class);

        AiServices<AiCustomerService> builder = buildBaseCustomerService(systemPrompt, chatMemory, streamingChatModel);

        attachCustomerServiceRagAugmentor(builder, conversationId);
        return builder.build();
    }

    /**
     * 加载 AI 客服系统提示词
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
     * 构建客服对话记忆
     *
     * @param conversationId 会话 ID
     * @return 对话记忆
     */
    private MessageWindowChatMemory buildChatMemory(Long conversationId) {
        return MessageWindowChatMemory.builder()
                .id(conversationId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(AiServiceConstant.CHAT_MEMORY_MAX_MESSAGES)
                .build();
    }

    /**
     * 加载客服历史消息到记忆中
     *
     * @param conversationId 会话 ID
     * @param chatMemory 对话记忆
     */
    private void loadCustomerServiceChatHistory(Long conversationId, MessageWindowChatMemory chatMemory) {
        Boolean loaded = customerServiceMessageService.loadChatHistoryToMemory(
                conversationId, chatMemory, AiServiceConstant.CUSTOMER_SERVICE_HISTORY_LOAD_LIMIT);
        if (Boolean.FALSE.equals(loaded)) {
            chatMemory.add(UserMessage.from(AiServiceConstant.HISTORY_LOAD_FAILED_MESSAGE));
        }
    }

    /**
     * 构建客服基础 AI 服务配置
     *
     * @param systemPrompt 系统提示词
     * @param chatMemory 对话记忆
     * @param streamingChatModel 流式模型
     * @return AI 服务构建器
     */
    private AiServices<AiCustomerService> buildBaseCustomerService(String systemPrompt,
                                                                   MessageWindowChatMemory chatMemory,
                                                                   StreamingChatModel streamingChatModel) {
        return AiServices.builder(AiCustomerService.class)
                .streamingChatModel(streamingChatModel)
                .systemMessageProvider(memoryId -> systemPrompt)
                .chatMemory(chatMemory)
                .chatMemoryProvider(id -> chatMemory);
    }

    /**
     * 为客服模型挂载 RAG 检索增强器
     *
     * @param builder AI 服务构建器
     * @param conversationId 会话 ID
     */
    private void attachCustomerServiceRagAugmentor(AiServices<AiCustomerService> builder, Long conversationId) {
        if (ragContentRetrieverAugmentorFactory == null) {
            log.debug(CUSTOMER_SERVICE_RAG_DISABLED_LOG_TEMPLATE, conversationId);
            return;
        }
        RetrievalAugmentor retrievalAugmentor = ragContentRetrieverAugmentorFactory
                .createCustomerServiceRetrievalAugmentor();
        builder.retrievalAugmentor(retrievalAugmentor);
    }
}
