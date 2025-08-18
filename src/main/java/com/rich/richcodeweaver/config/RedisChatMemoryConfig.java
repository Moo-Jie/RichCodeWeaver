package com.rich.richcodeweaver.config;

import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
     * redis 整合 Langchain4j 配置
     *
     * @return dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore
     * @author DuRuiChi
     * @create 2025/8/18
     **/
    @Bean
    public RedisChatMemoryStore redisChatMemoryStore() {
        return RedisChatMemoryStore.builder()
                .host(host)
                .port(port)
                // 账户密码上线时打开
//                .user(user)
//                .password(password)
                .ttl(ttl)
                .build();
    }
}
