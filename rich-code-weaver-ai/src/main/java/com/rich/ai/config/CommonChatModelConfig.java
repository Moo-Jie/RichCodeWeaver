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
 * @create 2025/12/24
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
     * 用于AI代码审查服务，分析代码质量、发现潜在问题
     * 使用原型模式，每次调用都创建一个新的实例，避免多线程并发问题
     *
     * @return dev.langchain4j.model.chat.ChatModel 聊天模型实例
     * @author DuRuiChi
     * @create 2025/12/24
     * @Scope("prototype") 原型模式，每次调用都创建一个新的实例
     **/
    @Bean
    @Scope("prototype")
    public ChatModel codeReviewChatModel() {
        // 使用通用构建方法创建聊天模型
        return buildChatModel();
    }

    /**
     * 图片资源收集模型（支持多例模式）
     * 用于AI图片资源收集服务，智能搜索和筛选图片资源
     * 使用原型模式，每次调用都创建一个新的实例，避免多线程并发问题
     *
     * @return dev.langchain4j.model.chat.ChatModel 聊天模型实例
     * @author DuRuiChi
     * @create 2026/1/19
     * @Scope("prototype") 原型模式，每次调用都创建一个新的实例
     **/
    @Bean
    @Scope("prototype")
    public ChatModel imageResourceChatModel() {
        // 使用通用构建方法创建聊天模型
        return buildChatModel();
    }

    /**
     * 网页资源收集模型（支持多例模式）
     * 用于AI网页资源收集服务，整理和组织网络资源
     * 使用原型模式，每次调用都创建一个新的实例，避免多线程并发问题
     *
     * @return dev.langchain4j.model.chat.ChatModel 聊天模型实例
     * @author DuRuiChi
     * @create 2025/12/24
     * @Scope("prototype") 原型模式，每次调用都创建一个新的实例
     **/
    @Bean
    @Scope("prototype")
    public ChatModel webResourceOrganizeChatModel() {
        // 使用通用构建方法创建聊天模型
        return buildChatModel();
    }

    /**
     * 代码生成策略选择模型（支持多例模式）
     * 用于AI代码生成策略选择服务，智能选择最佳的代码生成方案
     * 使用原型模式，每次调用都创建一个新的实例，避免多线程并发问题
     *
     * @return dev.langchain4j.model.chat.ChatModel 聊天模型实例
     * @author DuRuiChi
     * @create 2026/1/5
     * @Scope("prototype") 原型模式，每次调用都创建一个新的实例
     **/
    @Bean
    @Scope("prototype")
    public ChatModel codeGeneratorTypeStrategyChatModel() {
        // 使用通用构建方法创建聊天模型
        return buildChatModel();
    }

    /**
     * 提示词优化模型（支持多例模式）
     * 用于AI提示词优化服务，优化用户输入的提示词以获得更好的生成效果
     * 使用原型模式，每次调用都创建一个新的实例，避免多线程并发问题
     *
     * @return dev.langchain4j.model.chat.ChatModel 聊天模型实例
     * @author DuRuiChi
     * @create 2025/11/20
     * @Scope("prototype") 原型模式，每次调用都创建一个新的实例
     **/
    @Bean
    @Scope("prototype")
    public ChatModel promptOptimizationChatModel() {
        // 使用通用构建方法创建聊天模型
        return buildChatModel();
    }
    
    /**
     * 通用的聊天模型构建方法
     * 统一处理所有聊天模型的配置和参数校验，避免代码重复
     * 
     * @return dev.langchain4j.model.chat.ChatModel 聊天模型实例
     */
    private ChatModel buildChatModel() {
        // 构建OpenAI聊天模型
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder();
        
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
        
        // 设置超时时间（可选参数，防止请求长时间挂起）
        if (timeout != null && timeout > 0) {
            builder.timeout(Duration.ofMillis(timeout));
        }
        
        // 设置最大Token数（可选参数，控制生成内容的长度）
        if (maxTokens != null && maxTokens > 0) {
            builder.maxTokens(maxTokens);
        }
        
        // 设置是否记录请求日志（可选参数，用于调试）
        if (logRequests != null) {
            builder.logRequests(logRequests);
        }
        
        // 设置是否记录响应日志（可选参数，用于调试）
        if (logResponses != null) {
            builder.logResponses(logResponses);
        }
        
        // 构建并返回聊天模型实例
        return builder.build();
    }
}
