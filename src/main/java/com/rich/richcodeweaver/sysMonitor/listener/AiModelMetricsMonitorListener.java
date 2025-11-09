package com.rich.richcodeweaver.sysMonitor.listener;

import com.rich.richcodeweaver.sysMonitor.MetricsCollector;
import com.rich.richcodeweaver.sysMonitor.SysMonitorContextHolder;
import com.rich.richcodeweaver.sysMonitor.context.SysMonitorContext;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * AI模型指标监控监听器
 * （实现 ChatModelListener 接口，用于监控AI模型在请求、响应和错误时调用指标收集器）
 *
 * @author DuRuiChi
 * @create 2025/11/9
 **/
@Component
@Slf4j
public class AiModelMetricsMonitorListener implements ChatModelListener {

    // 用于在请求属性中存储请求开始时间的键
    private static final String REQUEST_START_TIME_KEY = "request_start_time";

    // 用于在请求属性中存储监控上下文的键
    // 由于请求和响应事件可能在不同线程中触发，需要通过属性传递上下文信息
    private static final String MONITOR_CONTEXT_KEY = "monitor_context";

    // 自定义指标收集器，用于收集各种监控指标并提交至指标注册中心，开放给 Prometheus
    @Resource
    private MetricsCollector metricsCollector;

    /**
     * 当AI模型请求开始时调用的方法
     *
     * @param requestContext 包含请求相关信息的上下文对象
     */
    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        // 记录请求开始时间到请求属性中，用于后续计算响应时间
        requestContext.attributes().put(REQUEST_START_TIME_KEY, Instant.now());

        // 从线程局部的监控上下文持有者中获取当前请求的监控上下文
        SysMonitorContext context = SysMonitorContextHolder.getContext();
        String userId = context.getUserId();  // 获取用户ID
        String appId = context.getAppId();    // 获取应用ID

        // 将监控上下文存储到请求属性中，以便在响应和错误处理时使用
        requestContext.attributes().put(MONITOR_CONTEXT_KEY, context);

        // 从请求中获取模型名称
        String modelName = requestContext.chatRequest().modelName();

        // 记录AI模型请求开始指标，状态为"started"
        metricsCollector.recordAiModelRequest(userId, appId, modelName, "started");
    }

    /**
     * 当AI模型成功响应时调用的方法
     *
     * @param responseContext 包含响应相关信息的上下文对象
     */
    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        // 从响应上下文中获取之前存储的属性
        Map<Object, Object> attributes = responseContext.attributes();

        // 从属性中获取之前存储的监控上下文
        SysMonitorContext context = (SysMonitorContext) attributes.get(MONITOR_CONTEXT_KEY);
        String userId = context.getUserId();  // 获取用户ID
        String appId = context.getAppId();    // 获取应用ID

        // 从响应中获取模型名称
        String modelName = responseContext.chatResponse().modelName();

        // 记录成功的AI模型请求指标，状态为"success"
        metricsCollector.recordAiModelRequest(userId, appId, modelName, "success");

        // 记录响应时间指标
        recordResponseTime(attributes, userId, appId, modelName);

        // 记录Token使用情况指标
        recordTokenUsage(responseContext, userId, appId, modelName);
    }

    /**
     * 当AI模型请求发生错误时调用的方法
     *
     * @param errorContext 包含错误相关信息的上下文对象
     */
    @Override
    public void onError(ChatModelErrorContext errorContext) {
        // 从线程局部的监控上下文持有者中获取当前请求的监控上下文
        SysMonitorContext context = SysMonitorContextHolder.getContext();
        String userId = context.getUserId();  // 获取用户ID
        String appId = context.getAppId();    // 获取应用ID

        // 从错误上下文中获取模型名称和错误信息
        String modelName = errorContext.chatRequest().modelName();
        String errorMessage = errorContext.error().getMessage();

        // 记录错误的AI模型请求指标，状态为"error"
        metricsCollector.recordAiModelRequest(userId, appId, modelName, "error");

        // 记录具体的错误信息指标
        metricsCollector.recordAiModelError(userId, appId, modelName, errorMessage);

        // 记录响应时间指标（即使是错误响应也记录）
        Map<Object, Object> attributes = errorContext.attributes();
        recordResponseTime(attributes, userId, appId, modelName);
    }

    /**
     * 记录响应时间的私有辅助方法
     *
     * @param attributes 请求属性映射，包含请求开始时间
     * @param userId     用户ID
     * @param appId      应用ID
     * @param modelName  AI模型名称
     */
    private void recordResponseTime(Map<Object, Object> attributes, String userId, String appId, String modelName) {
        // 从属性中获取请求开始时间
        Instant startTime = (Instant) attributes.get(REQUEST_START_TIME_KEY);

        // 计算响应时间（从开始时间到当前时间的持续时间）
        Duration responseTime = Duration.between(startTime, Instant.now());

        // 记录响应时间指标
        metricsCollector.recordAiModelResponseTime(userId, appId, modelName, responseTime);
    }

    /**
     * 记录Token使用情况的私有辅助方法
     *
     * @param responseContext 响应上下文，包含Token使用信息
     * @param userId          用户ID
     * @param appId           应用ID
     * @param modelName       AI模型名称
     */
    private void recordTokenUsage(ChatModelResponseContext responseContext, String userId, String appId, String modelName) {
        // 从响应元数据中获取Token使用情况
        TokenUsage tokenUsage = responseContext.chatResponse().metadata().tokenUsage();

        // 如果Token使用信息存在，则记录各项指标
        if (tokenUsage != null) {
            // 记录输入Token数量
            metricsCollector.recordAiModelTokenUsage(userId, appId, modelName, "input", tokenUsage.inputTokenCount());
            // 记录输出Token数量
            metricsCollector.recordAiModelTokenUsage(userId, appId, modelName, "output", tokenUsage.outputTokenCount());
            // 记录总Token数量
            metricsCollector.recordAiModelTokenUsage(userId, appId, modelName, "total", tokenUsage.totalTokenCount());
        }
    }
}