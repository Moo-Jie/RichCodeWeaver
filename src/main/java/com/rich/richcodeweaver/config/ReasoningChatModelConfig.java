package com.rich.richcodeweaver.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义的推理模型 （流式），用于生成复杂项目工程代码
 * 【需要写配置类，因为默认没有提供流式推理模型实例的类】
 *
 * @author DuRuiChi
 * @create 2025/8/24
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.reasoning-chat-model")
public class ReasoningChatModelConfig {
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
     * 推理模型 （流式），用于生成复杂项目工程代码
     *
     * @return dev.langchain4j.model.chat.StreamingChatModel
     * @author DuRuiChi
     * @create 2025/8/24
     **/
    @Bean
    public StreamingChatModel reasoningStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .modelName(modelName)
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .maxTokens(maxTokens)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }
}
