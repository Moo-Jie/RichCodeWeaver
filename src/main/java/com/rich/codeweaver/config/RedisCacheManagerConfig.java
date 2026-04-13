package com.rich.codeweaver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.Resource;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static com.rich.codeweaver.common.constant.CacheConstant.*;

/**
 * 基于 Redis 的缓存管理器配置类（使用 @Cacheable 注解配置接口将缓存到 Redis 中）
 * 将 Redis 配置为 Spring Cache 的默认缓存实现，替代默认的内存缓存
 *
 * @author DuRuiChi
 * @since 2026-03-09
 */
@Configuration
public class RedisCacheManagerConfig {

    @Resource
    private RedisConnectionFactory redisConnectionFactory;

    /**
     * 创建基于 Redis 缓存管理器 Bean，否则将使用默认的内存缓存管理器
     *
     * @return 配置完成的 CacheManager 实例
     * @author DuRuiChi
     */
    @Bean
    public CacheManager cacheManager() {
        // 配置支持 Java 8 时间类型的 Jackson 对象映射器
        // 确保 LocalDate、LocalDateTime 等类型能正确序列化到 Redis
        ObjectMapper objectMapper = createObjectMapperWithJavaTimeSupport();

        // 创建 Redis 缓存默认配置
        // 包括序列化方式、过期时间等通用设置
        RedisCacheConfiguration defaultConfig = createDefaultCacheConfig(objectMapper);

        // 构建基于 Redis 的缓存管理器
        return RedisCacheManager.builder(redisConnectionFactory)
                // 设置默认缓存配置（应用于所有未特别指定的缓存）
                .cacheDefaults(defaultConfig)
                // 设置特定缓存的配置：星标产物缓存使用更短的过期时间
                .withCacheConfiguration(STAR_APP_CACHE_NAME, defaultConfig.entryTtl(HOT_KEY_CACHE_TTL))
                .build();
    }

    /**
     * 创建支持 Java 8 时间类型的 Jackson ObjectMapper
     * 确保 LocalDate、LocalDateTime 等类型能正确序列化/反序列化
     *
     * @return 配置好的 ObjectMapper 实例
     * @author DuRuiChi
     */
    private ObjectMapper createObjectMapperWithJavaTimeSupport() {
        // 创建 Jackson 对象映射器
        ObjectMapper objectMapper = new ObjectMapper();

        // 注册 Java 8 时间模块，支持 LocalDate、LocalDateTime 等类型
        objectMapper.registerModule(new JavaTimeModule());

        // 禁用将日期写为时间戳，使用 ISO-8601 格式字符串（更可读）
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return objectMapper;
    }

    /**
     * 创建默认的 Redis 缓存配置
     * 包含序列化设置、过期时间等通用配置
     *
     * @param objectMapper 配置好的 Jackson 对象映射器
     * @return 默认缓存配置实例
     * @author DuRuiChi
     */
    private RedisCacheConfiguration createDefaultCacheConfig(ObjectMapper objectMapper) {
        return RedisCacheConfiguration.defaultCacheConfig()
                // 设置默认缓存过期时间（防止缓存永久存在）
                .entryTtl(DEFAULT_CACHE_TTL)
                // 禁止缓存 null 值，避免缓存穿透攻击
                .disableCachingNullValues()
                // 配置 Key 的序列化方式：使用字符串序列化（可读性好）
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                // 配置 Value 的序列化方式：使用 JSON 序列化（支持复杂对象）
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));
    }
}