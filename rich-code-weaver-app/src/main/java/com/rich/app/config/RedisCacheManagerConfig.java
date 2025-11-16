package com.rich.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.Resource;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static com.rich.common.constant.CacheConstant.*;

/**
 * 基于 Redis 的缓存管理器配置类（使用 @Cacheable 注解配置接口将缓存到 Redis 中）
 * 将 Redis 配置为 Spring Cache 的默认缓存实现，替代默认的内存缓存
 *
 * @author DuRuiChi
 * @create 2025/9/22
 **/
@Configuration
public class RedisCacheManagerConfig {

    @Resource
    private RedisConnectionFactory redisConnectionFactory;

    /**
     * 创建基于 Redis 缓存管理器 Bean，否则将使用默认的内存缓存管理器
     *
     * @return 配置完成的 CacheManager 实例
     */
    @Bean
    public CacheManager cacheManager() {
        // 配置支持 Java 8 时间类型的 Jackson 对象映射器
        ObjectMapper objectMapper = createObjectMapperWithJavaTimeSupport();

        // 创建 Redis 缓存默认配置
        RedisCacheConfiguration defaultConfig = createDefaultCacheConfig(objectMapper);

        // 构建基于 Redis 的缓存管理器
        return RedisCacheManager.builder(redisConnectionFactory)
                // 设置默认配置，指定缓存到 Redis 中
                .cacheDefaults(defaultConfig)
                // 设置针对特定键的配置
                .withCacheConfiguration(STAR_APP_CACHE_NAME, defaultConfig.entryTtl(HOT_KEY_CACHE_TTL))
                .build();
    }

    /**
     * 创建支持 Java 8 时间类型的 Jackson ObjectMapper
     * 确保 LocalDate、LocalDateTime 等类型能正确序列化/反序列化
     *
     * @return 配置好的 ObjectMapper 实例
     */
    private ObjectMapper createObjectMapperWithJavaTimeSupport() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    /**
     * 创建默认的 Redis 缓存配置
     * 包含序列化设置、过期时间等通用配置
     *
     * @param objectMapper 配置好的 Jackson 对象映射器
     * @return 默认缓存配置实例
     */
    private RedisCacheConfiguration createDefaultCacheConfig(ObjectMapper objectMapper) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_CACHE_TTL) // 设置默认缓存过期时间
                .disableCachingNullValues() // 禁止缓存空值，避免缓存穿透
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer())); // Key 使用字符串序列化

    }
}