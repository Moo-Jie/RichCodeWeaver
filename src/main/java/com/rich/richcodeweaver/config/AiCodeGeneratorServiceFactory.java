package com.rich.richcodeweaver.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rich.richcodeweaver.service.AiCodeGeneratorService;
import com.rich.richcodeweaver.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI 服务实例创建工厂，用于通过 AppId 创建 AI 服务实例
 * （在 Caffeine 缓存中创建实例、历史消息存入 Redis）
 *
 * @author DuRuiChi
 * @create 2025/8/5
 **/
@Slf4j
@Configuration
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * 创建 Caffeine 缓存服务，用于存储 AI 服务实例
     *
     * @param appId 应用 id
     * @return com.rich.richcodeweaver.service.AiCodeGeneratorService
     * @author DuRuiChi
     * @create 2025/8/18
     **/
    private final Cache<Long, AiCodeGeneratorService> caffeineService = Caffeine.newBuilder()
            // 防止内存泄露的配置
            // 最大缓存 1000 个实例
            .maximumSize(1000)
            // 写入后 30 分钟过期
            .expireAfterWrite(Duration.ofMinutes(30))
            // 访问后 10 分钟过期
            .expireAfterAccess(Duration.ofMinutes(10))
            // 移除时记录日志
            .removalListener((key, value, cause) -> {
                log.debug("【AI 服务实例缓存】已移除，appId: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 在 Caffeine 缓存中创建 AI 服务实例
     *
     * @return com.rich.richcodeweaver.service.AiCodeGeneratorService
     * @author DuRuiChi
     * @create 2025/8/5
     **/
    public AiCodeGeneratorService getAiCodeGeneratorService(Long appId) {
        // 从缓存中获取或创建 AI 服务实例：
        // 每次通过 AppId 来获取 AI 服务实例，加入 Caffeine 缓存
        // 若 appId 相同，会从缓存中获取已构建的实例，不会重复构建
        return caffeineService.get(appId, this::createAiCodeGeneratorService);
    }

    /**
     * 根据 appId 实现创建不同的独立 AI 服务，并从数据库加载对话历史
     *
     * @param appId
     * @return com.rich.richcodeweaver.service.AiCodeGeneratorService
     * @author DuRuiChi
     * @create 2025/8/18
     **/
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId) {
        log.info("为 appId: {} 创建新的 AI 服务实例", appId);
        // 根据 appId 创建独立的 chatMemory
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                // 指定为 Redis 类型的 ChatMemory
                .chatMemoryStore(redisChatMemoryStore)
                // 最大消息数
                .maxMessages(20)
                .build();
        // 从数据库中加载对话历史到 Redis 类型的 chatMemory 中
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 10);
        return AiServices.builder(AiCodeGeneratorService.class)
                // 配置 AI 模型
                .chatModel(chatModel)
                // 配置流式对话
                .streamingChatModel(streamingChatModel)
                // 配置 chatMemory
                .chatMemory(chatMemory)
                .build();
    }
}
