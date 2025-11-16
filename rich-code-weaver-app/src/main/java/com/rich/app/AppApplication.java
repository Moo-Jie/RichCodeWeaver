package com.rich.app;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
@SpringBootApplication(
        // 扫描所有 "com.rich" 包下的组件
        scanBasePackages = "com.rich",
        // 排除 RedisEmbeddingStoreAutoConfiguration，避免与 Redis 相关的自动配置冲突
        exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("com.rich.app.mapper")
@EnableCaching
public class AppApplication {
    public static void main(String[] args) {
        SpringApplication.run(AppApplication.class, args);
    }
}
