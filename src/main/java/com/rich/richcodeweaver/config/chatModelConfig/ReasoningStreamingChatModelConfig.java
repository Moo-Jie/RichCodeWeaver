package com.rich.richcodeweaver.config.chatModelConfig;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.time.Duration;

/**
 * 支持多例模式的推理流式模型（用于构建复杂的应用，如：构建工程化项目等）
 *
 * @author DuRuiChi
 * @create 2025/8/24
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.reasoning-streaming-chat-model")
public class ReasoningStreamingChatModelConfig {
    /**
     * 模型名称
     **/
    private String modelName;

    /**
     * 接口密钥
     **/
    private String apiKey;

    /**
     * 接口地址
     **/
    private String baseUrl;

    /**
     * 最大 Token 数
     **/
    private Integer maxTokens;

    /**
     * 开启请求日志
     **/
    private Boolean logRequests;

    /**
     * 开启响应日志
     **/
    private Boolean logResponses;

    /**
     * 超时时间
     **/
    private Integer timeout;

    /**
     * 推理流式模型（支持多例模式）
     *
     * @return dev.langchain4j.model.chat.StreamingChatModel
     * @author DuRuiChi
     * @create 2025/8/24
     * @Scope("prototype") 原型模式，每次调用都创建一个新的实例
     **/
    @Bean
    @Scope("prototype")
    public StreamingChatModel reasoningStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .modelName(modelName)
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .maxTokens(maxTokens)
                .logRequests(logRequests)
                .timeout(timeout != null ? Duration.ofMillis(timeout) : null)
                .logResponses(logResponses)
                .build();
    }
}
