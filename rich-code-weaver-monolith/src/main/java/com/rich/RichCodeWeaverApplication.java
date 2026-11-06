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
     * LangChain4j HTTP 客户端选择属性
     */
    private static final String HTTP_CLIENT_BUILDER_FACTORY_PROPERTY =
            "langchain4j.http.clientBuilderFactory";

    /**
     * JDK HttpClient 实现，兼容当前 Spring Boot 3.3.4 并避免双实现冲突
     */
    private static final String DEFAULT_HTTP_CLIENT_BUILDER_FACTORY =
            "dev.langchain4j.http.client.jdk.JdkHttpClientBuilderFactory";

    /**
     * 应用启动入口方法
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 依赖中同时存在 JDK 与 Spring RestClient 时，LangChain4j 必须显式选择实现
        System.setProperty(HTTP_CLIENT_BUILDER_FACTORY_PROPERTY,
                System.getProperty(HTTP_CLIENT_BUILDER_FACTORY_PROPERTY, DEFAULT_HTTP_CLIENT_BUILDER_FACTORY));
        SpringApplication.run(RichCodeWeaverApplication.class, args);
    }
}
