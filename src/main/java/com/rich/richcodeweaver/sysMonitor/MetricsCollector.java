package com.rich.richcodeweaver.sysMonitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 指标收集器，通过 Micrometer 库将指标暴露给监控系统（如 Prometheus）
 * （调用方法 ——> 更新存储在注册中心 MeterRegistry 中注册的指标信息 Counter ——> 等待监控系统 Prometheus 请求到指标信息）
 * 文档：https://www.cnblogs.com/yunlongn/p/11343848.html
 * @author DuRuiChi
 * @create 2025/11/8
 **/
@Component
@Slf4j
public class MetricsCollector {

    // 注入 spring-actuator 提供的 Micrometer 的指标注册中心，用于创建和注册指标
    @Resource
    private MeterRegistry meterRegistry;

    // 使用并发 ConcurrentMap 分业务缓存已创建的指标，避免重复创建相同的指标，提高性能
    private final ConcurrentMap<String, Counter> requestCounterCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> errorCounterCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> tokenUsageCounterCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> responseTimerCache = new ConcurrentHashMap<>();

    /**
     * 记录AI模型请求次数
     *
     * @param userId    用户ID标识
     * @param appId     应用ID标识
     * @param modelName AI模型名称
     * @param status    请求状态（如：success、error等）
     */
    public void recordAiModelRequest(String userId, String appId, String modelName, String status) {
        String cacheKey = generateRequestCacheKey(userId, appId, modelName, status);
        // 获取指标计数器，若不存在则创建并缓存
        Counter counter = requestCounterCache.computeIfAbsent(cacheKey, k ->
                Counter.builder("ai_model_requests_total")
                        .description("AI模型总请求次数")
                        // 添加标签，用于区分不同的指标（指标维度越多，监控更细粒度）
                        .tag("user_id", userId)
                        .tag("app_id", appId)
                        .tag("model_name", modelName)
                        .tag("status", status)
                        // 注册到指标注册中心并返回 Counter 实例
                        .register(meterRegistry)
        );
        counter.increment();
    }

    /**
     * 记录 AI 模型错误次数
     *
     * @param userId       用户ID标识
     * @param appId        应用ID标识
     * @param modelName    AI模型名称
     * @param errorMessage 错误信息描述
     */
    public void recordAiModelError(String userId, String appId, String modelName, String errorMessage) {
        String cacheKey = generateErrorCacheKey(userId, appId, modelName, errorMessage);
        // 获取指标计数器，若不存在则创建并缓存
        Counter counter = errorCounterCache.computeIfAbsent(cacheKey, k ->
                Counter.builder("ai_model_errors_total")
                        .description("AI模型错误次数")
                        // 添加标签，用于区分不同的指标（指标维度越多，监控更细粒度）
                        .tag("user_id", userId)
                        .tag("app_id", appId)
                        .tag("model_name", modelName)
                        .tag("error_message", errorMessage)
                        // 注册到指标注册中心并返回 Counter 实例
                        .register(meterRegistry)
        );
        counter.increment();
    }

    /**
     * 记录 AI 模型的 Token 消耗量
     *
     * @param userId     用户ID标识
     * @param appId      应用ID标识
     * @param modelName  AI模型名称
     * @param tokenType  Token类型（如：input、output）
     * @param tokenCount Token消耗数量
     */
    public void recordAiModelTokenUsage(String userId, String appId, String modelName,
                                        String tokenType, long tokenCount) {
        String cacheKey = generateTokenCacheKey(userId, appId, modelName, tokenType);
        // 获取指标计数器，若不存在则创建并缓存
        Counter counter = tokenUsageCounterCache.computeIfAbsent(cacheKey, k ->
                Counter.builder("ai_model_tokens_total")
                        .description("AI模型Token消耗总数")
                        // 添加标签，用于区分不同的指标（指标维度越多，监控更细粒度）
                        .tag("user_id", userId)
                        .tag("app_id", appId)
                        .tag("model_name", modelName)
                        .tag("token_type", tokenType)
                        // 注册到指标注册中心并返回 Counter 实例
                        .register(meterRegistry)
        );
        counter.increment(tokenCount);
    }

    /**
     * 记录AI模型响应时间
     *
     * @param userId    用户ID标识
     * @param appId     应用ID标识
     * @param modelName AI模型名称
     * @param duration  响应时间 Duration对象
     */
    public void recordAiModelResponseTime(String userId, String appId, String modelName, Duration duration) {
        String cacheKey = generateResponseTimeCacheKey(userId, appId, modelName);
        // 获取指标计数器，若不存在则创建并缓存
        Timer timer = responseTimerCache.computeIfAbsent(cacheKey, k ->
                Timer.builder("ai_model_response_duration_seconds")
                        .description("AI模型响应时间")
                        // 添加标签，用于区分不同的指标（指标维度越多，监控更细粒度）
                        .tag("user_id", userId)
                        .tag("app_id", appId)
                        .tag("model_name", modelName)
                        // 注册到指标注册中心并返回 Timer 实例
                        .register(meterRegistry)
        );
        timer.record(duration);
    }

    // 缓存键生成方法，提高可读性和可维护性
    private String generateRequestCacheKey(String userId, String appId, String modelName, String status) {
        return String.format("request_%s_%s_%s_%s", userId, appId, modelName, status);
    }

    private String generateErrorCacheKey(String userId, String appId, String modelName, String errorMessage) {
        return String.format("error_%s_%s_%s_%s", userId, appId, modelName, errorMessage);
    }

    private String generateTokenCacheKey(String userId, String appId, String modelName, String tokenType) {
        return String.format("token_%s_%s_%s_%s", userId, appId, modelName, tokenType);
    }

    private String generateResponseTimeCacheKey(String userId, String appId, String modelName) {
        return String.format("response_time_%s_%s_%s", userId, appId, modelName);
    }
}