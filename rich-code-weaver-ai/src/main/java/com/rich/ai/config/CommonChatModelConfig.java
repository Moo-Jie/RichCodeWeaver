package com.rich.ai.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.time.Duration;

/**
 * 支持多例模式的普通对话模型（用于简单的 AI 服务，如：代码审查、代码生成策略选择、网络资源收集、图片资源构建等）
 *
 * @author DuRuiChi
 * @create 2025/8/24
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
public class CommonChatModelConfig {
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
     * 超时设置- 推理模型需要更长的超时时间
     **/
    private Integer timeout;

    /**
     * 代码审查模型（支持多例模式）
     *
     * @return dev.langchain4j.model.chat.ChatModel
     * @author DuRuiChi
     * @create 2025/8/24
     * @Scope("prototype") 原型模式，每次调用都创建一个新的实例
     **/
    @Bean
    @Scope("prototype")
    public ChatModel codeReviewChatModel() {
        return OpenAiChatModel.builder()
                .modelName(modelName)
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .timeout(timeout != null ? Duration.ofMillis(timeout) : null)
                .maxTokens(maxTokens)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }

    /**
     * 图片资源收集模型（支持多例模式）
     *
     * @return dev.langchain4j.model.chat.ChatModel
     * @author DuRuiChi
     * @create 2025/9/19
     * @Scope("prototype") 原型模式，每次调用都创建一个新的实例
     **/
    @Bean
    @Scope("prototype")
    public ChatModel imageResourceChatModel() {
        return OpenAiChatModel.builder()
                .modelName(modelName)
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .maxTokens(maxTokens)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }

    /**
     * 网页资源收集模型（支持多例模式）
     *
     * @return dev.langchain4j.model.chat.ChatModel
     * @author DuRuiChi
     * @create 2025/8/24
     * @Scope("prototype") 原型模式，每次调用都创建一个新的实例
     **/
    @Bean
    @Scope("prototype")
    public ChatModel webResourceOrganizeChatModel() {
        return OpenAiChatModel.builder()
                .modelName(modelName)
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .maxTokens(maxTokens)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }

    /**
     * 代码生成策略选择模型（支持多例模式）
     *
     * @return dev.langchain4j.model.chat.ChatModel
     * @author DuRuiChi
     * @create 2025/9/5
     * @Scope("prototype") 原型模式，每次调用都创建一个新的实例
     **/
    @Bean
    @Scope("prototype")
    public ChatModel codeGeneratorTypeStrategyChatModel() {
        return OpenAiChatModel.builder()
                .modelName(modelName)
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .maxTokens(maxTokens)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }

    @Bean(name = "openAiChatModel")
    public ChatModel openAiChatModel() {
        return OpenAiChatModel.builder()
                .modelName(modelName)
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .timeout(timeout != null ? Duration.ofMillis(timeout) : null)
                .maxTokens(maxTokens)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }
}
