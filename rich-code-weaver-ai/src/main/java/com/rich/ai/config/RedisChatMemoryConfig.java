package com.rich.ai.config;

import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * redis 整合 Langchain4j 配置
 *
 * @author DuRuiChi
 * @create 2025/12/18
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
     * redis 账户
     */
    private String user;

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
     * Redis整合Langchain4j配置
     * 创建Redis聊天记忆存储，用于持久化AI对话历史
     * 支持TTL（生存时间）配置，自动清理过期的对话记录
     *
     * @return dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore Redis聊天记忆存储实例
     * @author DuRuiChi
     * @create 2025/12/18
     **/
    @Bean
    public RedisChatMemoryStore redisChatMemoryStore() {
        // 构建Redis聊天记忆存储
        RedisChatMemoryStore.Builder builder = RedisChatMemoryStore.builder();

        // 设置Redis主机地址（必需参数）
        if (host != null && !host.trim().isEmpty()) {
            builder.host(host);
        } else {
            // 如果未配置主机地址，使用默认值localhost
            builder.host("localhost");
        }
        
        // 设置Redis端口（必需参数）
        if (port > 0 && port <= 65535) {
            builder.port(port);
        } else {
            // 如果端口无效，使用默认值6379
            builder.port(6379);
        }
        
        // 设置Redis数据库索引（可选参数，默认为0）
        if (database >= 0 && database <= 15) {
//            builder.database(database);
        }
        
        // 设置用户名（可选参数，生产环境建议启用）
        // 注意：当前已注释，上线时需要取消注释以启用认证
        // if (user != null && !user.trim().isEmpty()) {
        //     builder.user(user);
        // }
        
        // 设置密码（可选参数，生产环境建议启用）
        // 注意：当前已注释，上线时需要取消注释以启用认证
        // if (password != null && !password.trim().isEmpty()) {
        //     builder.password(password);
        // }
        
        // 设置TTL（生存时间，单位：秒）
        // TTL用于自动清理过期的对话记录，避免Redis内存占用过高
        if (ttl > 0) {
            builder.ttl(ttl);
        }
        
        // 构建并返回Redis聊天记忆存储实例
        return builder.build();
    }
}
