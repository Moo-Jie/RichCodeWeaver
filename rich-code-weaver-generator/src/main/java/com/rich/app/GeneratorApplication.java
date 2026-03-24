package com.rich.app;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用模块启动类
 * 负责启动应用服务，提供应用管理、代码生成、聊天历史等核心功能
 * 
 * @author DuRuiChi
 * @since 2026-03-10
 */
@SpringBootApplication(
        scanBasePackages = "com.rich",
        exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("com.rich.app.mapper")
@EnableCaching
@EnableDubbo
@EnableScheduling
public class GeneratorApplication {
    
    /**
     * 应用启动入口方法
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GeneratorApplication.class, args);
    }
}
