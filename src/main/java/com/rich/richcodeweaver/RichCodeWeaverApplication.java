package com.rich.richcodeweaver;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("com.rich.richcodeweaver.mapper")
public class RichCodeWeaverApplication {

    public static void main(String[] args) {
        SpringApplication.run(RichCodeWeaverApplication.class, args);
    }

}
