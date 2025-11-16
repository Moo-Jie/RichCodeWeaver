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
 *
 * @author DuRuiChi
 * @create 2025/9/24
 **/
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
     * @return org.redisson.api.RedissonClient
     * @author DuRuiChi
     * @create 2025/9/24
     **/
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + redisHost + ":" + redisPort;
        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisDatabase)
                .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setConnectionPoolSize(connectionPoolSize)
                .setIdleConnectionTimeout(idleConnectionTimeout)
                .setConnectTimeout(connectTimeout)
                .setTimeout(timeout)
                .setRetryAttempts(retryAttempts)
                .setRetryInterval(retryInterval);
        // 如果有密码则设置密码
        if (redisPassword != null && !redisPassword.isEmpty()) {
            singleServerConfig.setPassword(redisPassword);
        }
        return Redisson.create(config);
    }
}
