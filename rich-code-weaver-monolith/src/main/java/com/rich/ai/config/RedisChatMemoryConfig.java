package com.rich.ai.config;

import com.rich.ai.memory.chat.ChunkedRedisChatMemoryStore;
import com.rich.common.constant.AiServiceConstant;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * redis 整合 Langchain4j 配置
 *
 * @author DuRuiChi
 * @create 2025/12/18
 **/
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Data
public class RedisChatMemoryConfig {

    /**
     * redis 过期时间
     */
    private long ttl;

    /**
     * 创建带分片保护的聊天记忆存储
     *
     * @return ChatMemoryStore 聊天记忆存储实例
     * @author DuRuiChi
     * @create 2025/12/18
     **/
    @Bean
    public ChatMemoryStore chatMemoryStore(StringRedisTemplate stringRedisTemplate) {
        return new ChunkedRedisChatMemoryStore(
                stringRedisTemplate,
                ttl,
                AiServiceConstant.CHAT_MEMORY_CHUNK_SIZE,
                AiServiceConstant.CHAT_MEMORY_MAX_CHUNKS_PER_MESSAGE,
                AiServiceConstant.CHAT_MEMORY_MAX_MESSAGE_CHARS,
                AiServiceConstant.CHAT_MEMORY_TRUNCATION_SUFFIX,
                AiServiceConstant.CHAT_MEMORY_TOOL_FOLD_KEEP_RECENT,
                AiServiceConstant.CHAT_MEMORY_TOOL_FOLDED_RESULT,
                AiServiceConstant.CHAT_MEMORY_TOOL_FOLDED_ARGUMENTS
        );
    }

    /**
     * Token 计数器（用于 TokenWindowChatMemory 按 Token 淘汰旧消息）
     * 使用 OpenAiTokenCountEstimator 近似计数，对 Qwen 等兼容模型同样适用
     *
     * @return TokenCountEstimator Token 计数器
     */
    @Bean
    public TokenCountEstimator chatMemoryTokenCountEstimator() {
        log.info("初始化 Token 计数器，近似模型: {}", AiServiceConstant.CHAT_MEMORY_TOKENIZER_MODEL);
        // OpenAiTokenCountEstimator 是 Langchain4j 内置的本地 Token 估算工具
        // 使用 OpenAI 官方为 gpt-4o 模型制定的 Token 计算标准，来做本地分词和计数，不是调用大模型
        // （Qwen3-Coder-Plus 采用的分词编码 = O200K_BASE，与 “GPT_4O("gpt-4o", EncodingType.O200K_BASE, 128000)” 完美适配）
        return new OpenAiTokenCountEstimator(AiServiceConstant.CHAT_MEMORY_TOKENIZER_MODEL);
    }
}
