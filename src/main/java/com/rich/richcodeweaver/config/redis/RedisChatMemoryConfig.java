package com.rich.richcodeweaver.config.redis;

import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * redis 整合 Langchain4j 配置
 *
 * @author DuRuiChi
 * @create 2025/8/18
 **/
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Data
public class RedisChatMemoryConfig {

    /**
     * redis 主机
     */
    private String host;

    /**
     * redis 密码
     */
    private String password;

    /**
     * redis 端口
     */
    private int port;

    /**
     * redis 数据库
     */
    private int database;

    /**
     * redis 过期时间
     */
    private long ttl;

    /**
     * redis 整合 Langchain4j 配置（对话记忆）
     * <p>
     * 使用 Spring Data Redis 实现，避免 langchain4j-community-redis 1.1.0-beta7 在未设置 user 时不携带 password，
     * 导致 Redis requirepass 场景下出现 NOAUTH。
     *
     * @return dev.langchain4j.store.memory.chat.ChatMemoryStore
     * @author DuRuiChi
     * @create 2025/8/18
     **/
    @Bean
    public ChatMemoryStore redisChatMemoryStore(StringRedisTemplate stringRedisTemplate) {
        // key 前缀可选：默认不加，保持与旧实现一致
        String prefix = "";
        return new SpringRedisChatMemoryStore(stringRedisTemplate, prefix, ttl);
    }
}
