package com.rich.richcodeweaver.config.redis;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 使用 Spring Data Redis 实现的 ChatMemoryStore。
 * <p>
 * 背景：langchain4j-community-redis 1.1.0-beta7 的 RedisChatMemoryStore 在 user 为空时不会传入 password，
 * 会导致 Redis requirepass 场景下出现 NOAUTH（未认证）。
 * <p>
 * 该实现复用 Spring 的 RedisConnectionFactory（即复用 spring.data.redis.* 认证配置），兼容 Redis 3/4/5/6+。
 */
@RequiredArgsConstructor
public class SpringRedisChatMemoryStore implements ChatMemoryStore {

    private final StringRedisTemplate stringRedisTemplate;
    private final String keyPrefix;
    /**
     * TTL（秒）。<=0 表示不过期
     */
    private final long ttlSeconds;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = stringRedisTemplate.opsForValue().get(toRedisKey(memoryId));
        if (json == null) {
            return new ArrayList<>();
        }
        return ChatMessageDeserializer.messagesFromJson(json);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String key = toRedisKey(memoryId);
        if (messages == null || messages.isEmpty()) {
            stringRedisTemplate.delete(key);
            return;
        }
        String json = ChatMessageSerializer.messagesToJson(messages);
        if (ttlSeconds > 0) {
            stringRedisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds));
        } else {
            stringRedisTemplate.opsForValue().set(key, json);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        stringRedisTemplate.delete(toRedisKey(memoryId));
    }

    private String toRedisKey(Object memoryId) {
        if (memoryId == null) {
            throw new IllegalArgumentException("memoryId 不能为空");
        }
        return (keyPrefix == null ? "" : keyPrefix) + memoryId;
    }
}


