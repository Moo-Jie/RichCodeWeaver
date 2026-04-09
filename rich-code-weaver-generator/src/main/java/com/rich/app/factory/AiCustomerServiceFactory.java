package com.rich.app.factory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rich.ai.rag.RagContentRetrieverAugmentorFactory;
import com.rich.ai.service.AiCustomerService;
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

    private static final String CUSTOMER_SERVICE_PROMPT_KEY = "customer-service-system-prompt";

    private final Cache<Long, AiCustomerService> caffeineCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
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

    public AiCustomerService getCustomerService(Long conversationId) {
        return caffeineCache.get(conversationId, key -> createCustomerService(conversationId));
    }

    private AiCustomerService createCustomerService(Long conversationId) {
        String systemPrompt = innerSystemPromptService.getPromptContentByKey(CUSTOMER_SERVICE_PROMPT_KEY);
        if (systemPrompt == null || systemPrompt.isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "未找到 AI 客服系统提示词，promptKey=" + CUSTOMER_SERVICE_PROMPT_KEY + "，请在管理后台配置");
        }

        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(conversationId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(50)
                .build();

        Boolean loaded = customerServiceMessageService.loadChatHistoryToMemory(conversationId, chatMemory, 20);
        if (Boolean.FALSE.equals(loaded)) {
            chatMemory.add(UserMessage.from("历史记录加载失败，可能已经过期。"));
        }

        StreamingChatModel customerServiceStreamingChatModel =
                SpringContextUtil.getBean("customerServiceStreamingChatModel", StreamingChatModel.class);

        AiServices<AiCustomerService> builder = AiServices.builder(AiCustomerService.class)
                .streamingChatModel(customerServiceStreamingChatModel)
                .systemMessageProvider(memoryId -> systemPrompt)
                .chatMemory(chatMemory)
                .chatMemoryProvider(id -> chatMemory);

        if (ragContentRetrieverAugmentorFactory != null) {
            RetrievalAugmentor retrievalAugmentor = ragContentRetrieverAugmentorFactory
                    .createCustomerServiceRetrievalAugmentor();
            builder.retrievalAugmentor(retrievalAugmentor);
        }
        return builder.build();
    }
}
