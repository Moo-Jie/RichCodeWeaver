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
 * 支持多例模式的普通流式模型（用于构建简单的产物，如：构建单文件产物、多文件产物等）
 *
 * @author DuRuiChi
 * @create 2025/12/24
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
     * 用于构建简单的产物，如单文件产物、多文件产物等
     * 使用原型模式，每次调用都创建一个新的实例，避免多线程并发问题
     *
     * @return dev.langchain4j.model.chat.StreamingChatModel 流式聊天模型实例
     * @author DuRuiChi
     * @create 2025/12/24
     * @Scope("prototype") 原型模式，每次调用都创建一个新的实例
     **/
    @Bean
    @Scope("prototype")
    public StreamingChatModel streamingChatModel() {
        // 构建OpenAI流式聊天模型
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder();

        // 设置模型名称（必需参数）
        if (modelName != null && !modelName.trim().isEmpty()) {
            builder.modelName(modelName);
        }

        // 设置API密钥（必需参数）
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            builder.apiKey(apiKey);
        }

        // 设置基础URL（可选参数，用于自定义API端点）
        if (baseUrl != null && !baseUrl.trim().isEmpty()) {
            builder.baseUrl(baseUrl);
        }

        // 设置最大Token数（可选参数，控制生成内容的长度）
        if (maxTokens != null && maxTokens > 0) {
            builder.maxTokens(maxTokens);
        }

        // 设置超时时间（可选参数，防止请求长时间挂起）
        if (timeout != null && timeout > 0) {
            builder.timeout(Duration.ofMillis(timeout));
        }

        // 设置温度参数（可选参数，控制输出的随机性，范围0-1）
        if (temperature != null && temperature >= 0.0 && temperature <= 1.0) {
            builder.temperature(temperature);
        }

        // 设置是否记录请求日志（可选参数，用于调试）
        if (logRequests != null) {
            builder.logRequests(logRequests);
        }

        // 设置是否记录响应日志（可选参数，用于调试）
        if (logResponses != null) {
            builder.logResponses(logResponses);
        }

        // 添加监听器（可选参数，用于监控模型指标）
        // 注意：当前已注释，如需启用请取消注释
        // if (aiModelMetricsMonitorListener != null) {
        //     builder.listeners(List.of(aiModelMetricsMonitorListener));
        // }

        // 构建并返回流式聊天模型实例
        return builder.build();
    }
}
