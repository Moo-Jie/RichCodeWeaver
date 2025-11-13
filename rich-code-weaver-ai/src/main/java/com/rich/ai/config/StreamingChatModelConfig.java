package com.rich.ai.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.time.Duration;

/**
 * 支持多例模式的普通流式模型（用于构建简单的应用，如：构建单文件应用、多文件应用等）
 *
 * @author DuRuiChi
 * @create 2025/8/24
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.streaming-chat-model")
public class StreamingChatModelConfig {
    /**
     * AI 模型指标监控监听器
     **/
//    @Resource
//    private AiModelMetricsMonitorListener aiModelMetricsMonitorListener;

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
     * 温度（0-1），较高的值会使输出更加随机，而较低的值则会使输出更加确定
     **/
    private Double temperature;

    /**
     * 普通流式模型（支持多例模式）
     *
     * @return dev.langchain4j.model.chat.StreamingChatModel
     * @author DuRuiChi
     * @create 2025/8/24
     * @Scope("prototype") 原型模式，每次调用都创建一个新的实例
     **/
    @Bean
    @Scope("prototype")
    public StreamingChatModel streamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .modelName(modelName)
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .maxTokens(maxTokens)
                .timeout(timeout != null ? Duration.ofMillis(timeout) : null)
                .temperature(temperature)
                .logRequests(logRequests)
                .logResponses(logResponses)
//                .listeners(List.of(aiModelMetricsMonitorListener))
                .build();
    }
}
