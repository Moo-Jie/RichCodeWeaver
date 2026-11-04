package com.rich;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * RichCodeWeaver 单体应用启动类
 *
 * @author DuRuiChi
 */
@SpringBootApplication(
        scanBasePackages = "com.rich",
        exclude = RedisEmbeddingStoreAutoConfiguration.class)
@MapperScan({
        "com.rich.app.mapper",
        "com.rich.user.mapper",
        "com.rich.prompt.mapper",
        "com.rich.social.mapper"
})
@EnableCaching
@EnableScheduling
@EnableAsync
public class RichCodeWeaverApplication {

    /**
     * 应用启动入口方法
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(RichCodeWeaverApplication.class, args);
    }
}
