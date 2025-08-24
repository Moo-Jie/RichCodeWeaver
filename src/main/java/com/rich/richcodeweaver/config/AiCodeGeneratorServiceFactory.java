package com.rich.richcodeweaver.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rich.richcodeweaver.model.enums.CodeGeneratorTypeEnum;
import com.rich.richcodeweaver.service.AiCodeGeneratorService;
import com.rich.richcodeweaver.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
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

    /**
     * 基础模型
     **/
    @Resource
    private ChatModel chatModel;

    /**
     * 基础流式模型，用于生成多文件代码
     **/
    @Resource
    private StreamingChatModel openAiStreamingChatModel;

    /**
     * 自定义流式推理模型，用于生成复杂的项目工程代码
     **/
    @Resource
    private StreamingChatModel reasoningStreamingChatModel;

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
    public AiCodeGeneratorService getAiCodeGeneratorService(Long appId, CodeGeneratorTypeEnum codeGenTypeEnum) {
        // 从缓存中获取或创建 AI 服务实例:
        // 每次通过 AppId 来获取 AI 服务实例，加入 Caffeine 缓存
        // 若 appId 相同，会从缓存中获取已构建的实例，从而避免重复从数据库查询历史对话记录构建实例
        return caffeineService.get(appId, (key) -> this.createAiCodeGeneratorService(key, codeGenTypeEnum));
    }

    /**
     * 根据 appId 实现创建不同的独立 AI 服务，并从数据库加载对话历史
     *
     * @param appId
     * @return com.rich.richcodeweaver.service.AiCodeGeneratorService
     * @author DuRuiChi
     * @create 2025/8/18
     **/
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGeneratorTypeEnum codeGenTypeEnum) {
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
        Boolean isSave = chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 10);
        // 若加载失败，记录日志
        if (!isSave) {
            chatMemory.add(UserMessage.from("历史记录加载失败，可能已经过期。"));
            log.info("为 appId: {} 加载历史记录失败", appId);
        }
        // 分模式构建 AI 服务实例
        AiServices<AiCodeGeneratorService> aiCodeGenServices = AiServices.builder(AiCodeGeneratorService.class)
                // 配置 AI 模型
                .chatModel(chatModel)
                // 配置 chatMemory
                .chatMemory(chatMemory);
        switch (codeGenTypeEnum) {
            // 单文件模式、多文件模式
            case HTML, MULTI_FILE -> {
                return aiCodeGenServices
                        // 配置流式模型
                        .streamingChatModel(openAiStreamingChatModel)
                        .build();
            }
            // Vue 项目工程模式
            case VUE_PROJECT -> {
                return aiCodeGenServices
                        // 配置自定义推理流式模型
                        .streamingChatModel(reasoningStreamingChatModel)
                        // 当前模式使用了 memoryId ,强制要求指定 chatMemoryProvider
                        .chatMemoryProvider(id -> chatMemory)
                        // 当幻觉调用工具名称时，使用自定义策略
                        // 参考 ：https://blog.csdn.net/qq_52155674/article/details/147238250
                        .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                                toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()))

                        .build();
            }
            default -> {
                return aiCodeGenServices.build();
            }
        }
    }
}
