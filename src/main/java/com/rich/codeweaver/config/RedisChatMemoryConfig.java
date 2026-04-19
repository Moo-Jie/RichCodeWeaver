package com.rich.codeweaver.config;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson;

/**
 * redis 整合 Langchain4j 配置
 *
 * @author DuRuiChi
 * @create 2025/12/18
 **/
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
public class RedisChatMemoryConfig {

    private static final String CHAT_MEMORY_KEY_PREFIX = "langchain4j:chat_memory:";

    /**
     * redis 过期时间
     */
    private long ttl;

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    /**
     * Redis整合Langchain4j配置
     * 创建Redis聊天记忆存储，用于持久化AI对话历史
     * 支持TTL（生存时间）配置，自动清理过期的对话记录
     *
     * @return dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore Redis聊天记忆存储实例
     * @author DuRuiChi
     * @create 2025/12/18
     **/
    @Bean
    public ChatMemoryStore redisChatMemoryStore(StringRedisTemplate stringRedisTemplate) {
        return new ChatMemoryStore() {
            @Override
            public List<ChatMessage> getMessages(Object memoryId) {
                String json = stringRedisTemplate.opsForValue().get(buildChatMemoryKey(memoryId));
                if (json == null || json.isBlank()) {
                    return Collections.emptyList();
                }
                return messagesFromJson(json);
            }

            @Override
            public void updateMessages(Object memoryId, List<ChatMessage> messages) {
                String chatMemoryKey = buildChatMemoryKey(memoryId);
                String json = messagesToJson(messages);
                if (ttl > 0) {
                    stringRedisTemplate.opsForValue().set(chatMemoryKey, json, ttl, TimeUnit.SECONDS);
                    return;
                }
                stringRedisTemplate.opsForValue().set(chatMemoryKey, json);
            }

            @Override
            public void deleteMessages(Object memoryId) {
                stringRedisTemplate.delete(buildChatMemoryKey(memoryId));
            }
        };
    }

    private String buildChatMemoryKey(Object memoryId) {
        return CHAT_MEMORY_KEY_PREFIX + memoryId;
    }
}
