package com.rich.richcodeweaver;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * 主应用类
 *
 * @author DuRuiChi
 * @create 2025/9/22
 **/
@EnableCaching // 开启缓存
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class}) // 排除 RedisEmbeddingStoreAutoConfiguration，避免与 Redis 相关的自动配置冲突
@MapperScan("com.rich.richcodeweaver.mapper") // 扫描mapper接口
public class RichCodeWeaverApplication {

    public static void main(String[] args) {
        SpringApplication.run(RichCodeWeaverApplication.class, args);
    }

}
