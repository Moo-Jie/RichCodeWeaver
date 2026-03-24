package com.rich.app.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置类
 * 配置Redisson客户端，用于实现分布式锁、限流等功能
 *
 * @author DuRuiChi
 * @since 2026-03-09
 */
@Configuration
public class RedissonConfig {

    // Redis服务器地址
    @Value("${spring.data.redis.host}")
    private String redisHost;

    // Redis服务器端口
    @Value("${spring.data.redis.port}")
    private Integer redisPort;

    // Redis访问密码
    @Value("${spring.data.redis.password}")
    private String redisPassword;

    // Redis数据库索引
    @Value("${spring.data.redis.database}")
    private Integer redisDatabase;

    // 连接池最大连接数
    @Value("${redisson.connectionPoolSize}")
    private Integer connectionPoolSize;

    // 最小空闲连接数
    @Value("${redisson.connectionMinimumIdleSize}")
    private Integer connectionMinimumIdleSize;

    // 空闲连接超时时间（毫秒）
    @Value("${redisson.idleConnectionTimeout}")
    private Integer idleConnectionTimeout;

    // 连接超时时间（毫秒）
    @Value("${redisson.connectTimeout}")
    private Integer connectTimeout;

    // 命令等待超时时间（毫秒）
    @Value("${redisson.timeout}")
    private Integer timeout;

    // 命令重试次数
    @Value("${redisson.retryAttempts}")
    private Integer retryAttempts;

    // 命令重试间隔时间（毫秒）
    @Value("${redisson.retryInterval}")
    private Integer retryInterval;


    /**
     * 创建 Redisson 客户端
     *
     * @return Redisson客户端实例
     * @author DuRuiChi
     */
    @Bean
    public RedissonClient redissonClient() {
        // 创建 Redisson 配置对象
        Config config = new Config();
        
        // 构建 Redis 服务器地址（格式：redis://host:port）
        String address = "redis://" + redisHost + ":" + redisPort;
        
        // 配置单机模式的 Redis 服务器参数
        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress(address)  // 设置 Redis 服务器地址
                .setDatabase(redisDatabase)  // 设置数据库索引
                .setConnectionMinimumIdleSize(connectionMinimumIdleSize)  // 最小空闲连接数
                .setConnectionPoolSize(connectionPoolSize)  // 连接池大小
                .setIdleConnectionTimeout(idleConnectionTimeout)  // 空闲连接超时时间
                .setConnectTimeout(connectTimeout)  // 连接超时时间
                .setTimeout(timeout)  // 命令执行超时时间
                .setRetryAttempts(retryAttempts)  // 命令重试次数
                .setRetryInterval(retryInterval);  // 命令重试间隔时间
        
        // 如果配置了 Redis 密码，则设置密码（排除 null、空字符串和纯空白字符串）
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            singleServerConfig.setPassword(redisPassword);
        }
        
        // 创建并返回 Redisson 客户端实例
        return Redisson.create(config);
    }
}
