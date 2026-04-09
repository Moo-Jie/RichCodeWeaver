package com.rich.ai.config;

import com.rich.ai.monitor.AiModelMonitorListener;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.time.Duration;
import java.util.List;

/**
 * AI 客服专用流式模型配置
 *
 * @author DuRuiChi
 * @create 2026/4/9
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.customer-service-streaming-chat-model")
public class CustomerServiceChatModelConfig {

    @Resource
    private AiModelMonitorListener aiModelMonitorListener;

    private String modelName;

    private String apiKey;

    private String baseUrl;

    private Integer maxTokens;

    private Boolean logRequests;

    private Boolean logResponses;

    private Integer timeout;

    private Double temperature;

    @Bean(name = "customerServiceStreamingChatModel")
    @Scope("prototype")
    public StreamingChatModel customerServiceStreamingChatModel() {
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder();

        if (modelName != null && !modelName.trim().isEmpty()) {
            builder.modelName(modelName);
        }
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            builder.apiKey(apiKey);
        }
        if (baseUrl != null && !baseUrl.trim().isEmpty()) {
            builder.baseUrl(baseUrl);
        }
        if (maxTokens != null && maxTokens > 0) {
            builder.maxTokens(maxTokens);
        }
        if (timeout != null && timeout > 0) {
            builder.timeout(Duration.ofMillis(timeout));
        }
        if (temperature != null && temperature >= 0.0 && temperature <= 1.0) {
            builder.temperature(temperature);
        }
        if (logRequests != null) {
            builder.logRequests(logRequests);
        }
        if (logResponses != null) {
            builder.logResponses(logResponses);
        }
        if (aiModelMonitorListener != null) {
            builder.listeners(List.of(aiModelMonitorListener));
        }
        return builder.build();
    }
}
