package com.rich.richcodeweaver;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// 排除 Embedding 的自动配置，后续使用 RAG 时再开启
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("com.rich.richcodeweaver.mapper")
public class RichCodeWeaverApplication {

    public static void main(String[] args) {
        SpringApplication.run(RichCodeWeaverApplication.class, args);
    }

}
