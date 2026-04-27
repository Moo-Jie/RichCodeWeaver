package com.rich.ai.monitor;

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
 * AI 模型指标收集器
 * 负责收集 AI 模型调用的各类业务指标（包括请求次数、错误次数、Token 消耗、响应时间），统计后以暴露端点的形式上报给 Prometheus。
 *
 * @author DuRuiChi
 * @create 2026/03/31
 * @see AiModelMonitorListener
 **/
@Component
@Slf4j
public class AiModelMetricsCollector {

    /**
     * Micrometer 指标注册表
     * 用于注册和管理各类监控指标，由 Spring Boot Actuator 自动配置。
     * 所有通过该注册表创建的指标都会自动暴露到 /actuator/prometheus 端点。
     */
    @Resource
    private MeterRegistry meterRegistry;

    /**
     * 请求计数器缓存
     * 缓存已创建的请求计数器，避免重复创建相同维度的 Counter 对象。
     */
    private final ConcurrentMap<String, Counter> requestCountersCache = new ConcurrentHashMap<>();

    /**
     * 错误计数器缓存
     * 缓存已创建的错误计数器，避免重复创建相同维度的 Counter 对象。
     */
    private final ConcurrentMap<String, Counter> errorCountersCache = new ConcurrentHashMap<>();

    /**
     * Token 计数器缓存
     * 缓存已创建的 Token 计数器，避免重复创建相同维度的 Counter 对象。
     * Key 格式：userId_appId_modelName_genMode_tokenType
     */
    private final ConcurrentMap<String, Counter> tokenCountersCache = new ConcurrentHashMap<>();

    /**
     * 响应时间计时器缓存
     * 缓存已创建的响应时间计时器，避免重复创建相同维度的 Timer 对象。
     */
    private final ConcurrentMap<String, Timer> responseTimersCache = new ConcurrentHashMap<>();

    /**
     * 记录 AI 模型请求次数
     * 统计 AI 模型的总请求次数，支持按状态分类统计。
     *
     * @param userId    用户ID，用于按用户维度统计
     * @param appId     产物ID，用于按产物维度统计
     * @param modelName 模型名称
     * @param genMode   代码生成模式，"workflow" 或 "agent"
     * @param status    请求状态，"started"（开始）、"success"（成功）、"error"（失败）
     */
    public void recordRequest(String userId, String appId, String modelName, String genMode, String status) {
        // 参数校验：确保所有参数都不为空，避免创建无效的指标
        if (userId == null || appId == null || modelName == null || genMode == null || status == null) {
            log.warn("记录请求次数时参数不完整，跳过记录: userId={}, appId={}, modelName={}, genMode={}, status={}",
                    userId, appId, modelName, genMode, status);
            return;
        }

        try {
            // 生成缓存键：使用下划线连接所有维度，确保唯一性
            String cacheKey = String.format("%s_%s_%s_%s_%s", userId, appId, modelName, genMode, status);

            // 从缓存中获取或创建 Counter
            // computeIfAbsent 是线程安全的，多个线程同时调用时只会创建一次
            Counter counter = requestCountersCache.computeIfAbsent(cacheKey, key ->
                    Counter.builder("ai_model_requests")
                            .description("AI模型总请求次数")
                            .tag("user_id", userId)
                            .tag("app_id", appId)
                            .tag("model_name", modelName)
                            .tag("gen_mode", genMode)
                            .tag("status", status)
                            .register(meterRegistry)
            );

            // 计数器加 1
            counter.increment();

            log.debug("记录请求次数: userId={}, appId={}, modelName={}, genMode={}, status={}",
                    userId, appId, modelName, genMode, status);
        } catch (Exception e) {
            // 捕获异常，避免监控逻辑影响业务流程
            log.error("记录请求次数失败: userId={}, appId={}, modelName={}, genMode={}, status={}",
                    userId, appId, modelName, genMode, status, e);
        }
    }

    /**
     * 记录 AI 模型错误次数
     * 统计 AI 模型调用失败的次数和错误类型。
     *
     * @param userId       用户ID，用于按用户维度统计
     * @param appId        产物ID，用于按产物维度统计
     * @param modelName    模型名称
     * @param genMode      代码生成模式，"workflow" 或 "agent"
     * @param errorMessage 错误信息，建议使用简短的错误类型而非完整的堆栈信息
     */
    public void recordError(String userId, String appId, String modelName, String genMode, String errorMessage) {
        // 参数校验：确保所有参数都不为空
        if (userId == null || appId == null || modelName == null || genMode == null || errorMessage == null) {
            log.warn("记录错误次数时参数不完整，跳过记录: userId={}, appId={}, modelName={}, genMode={}, errorMessage={}",
                    userId, appId, modelName, genMode, errorMessage);
            return;
        }

        try {
            // 错误信息截断：避免错误信息过长导致标签基数爆炸
            // Prometheus 建议标签值不要超过 100 个字符
            String truncatedError = errorMessage.length() > 100
                    ? errorMessage.substring(0, 100) + "..."
                    : errorMessage;

            // 生成缓存键
            String cacheKey = String.format("%s_%s_%s_%s_%s", userId, appId, modelName, genMode, truncatedError);

            // 从缓存中获取或创建 Counter
            Counter counter = errorCountersCache.computeIfAbsent(cacheKey, key ->
                    Counter.builder("ai_model_errors")
                            .description("AI模型错误次数")
                            .tag("user_id", userId)
                            .tag("app_id", appId)
                            .tag("model_name", modelName)
                            .tag("gen_mode", genMode)
                            .tag("error_message", truncatedError)
                            .register(meterRegistry)
            );

            // 计数器加 1
            counter.increment();

            log.debug("记录错误次数: userId={}, appId={}, modelName={}, genMode={}, errorMessage={}",
                    userId, appId, modelName, genMode, truncatedError);
        } catch (Exception e) {
            // 捕获异常，避免监控逻辑影响业务流程
            log.error("记录错误次数失败: userId={}, appId={}, modelName={}, genMode={}, errorMessage={}",
                    userId, appId, modelName, genMode, errorMessage, e);
        }
    }

    /**
     * 记录 AI 模型 Token 消耗
     * 统计 AI 模型的 Token 使用情况，支持按类型分类统计。
     *
     * @param userId     用户ID，用于按用户维度统计
     * @param appId      产物ID，用于按产物维度统计
     * @param modelName  模型名称
     * @param genMode    代码生成模式，"workflow" 或 "agent"
     * @param tokenType  Token 类型，"input"（输入）、"output"（输出）、"total"（总计）
     * @param tokenCount Token 数量，必须为非负数
     */
    public void recordTokenUsage(String userId, String appId, String modelName, String genMode,
                                 String tokenType, long tokenCount) {
        // 参数校验：确保所有参数都不为空，且 Token 数量为非负数
        if (userId == null || appId == null || modelName == null || genMode == null || tokenType == null) {
            log.warn("记录Token消耗时参数不完整，跳过记录: userId={}, appId={}, modelName={}, genMode={}, tokenType={}, tokenCount={}",
                    userId, appId, modelName, genMode, tokenType, tokenCount);
            return;
        }

        if (tokenCount < 0) {
            log.warn("Token数量不能为负数，跳过记录: userId={}, appId={}, tokenCount={}",
                    userId, appId, tokenCount);
            return;
        }

        try {
            // 生成缓存键
            String cacheKey = String.format("%s_%s_%s_%s_%s", userId, appId, modelName, genMode, tokenType);

            // 从缓存中获取或创建 Counter
            Counter counter = tokenCountersCache.computeIfAbsent(cacheKey, key ->
                    Counter.builder("ai_model_tokens")
                            .description("AI模型Token消耗总数")
                            .tag("user_id", userId)
                            .tag("app_id", appId)
                            .tag("model_name", modelName)
                            .tag("gen_mode", genMode)
                            .tag("token_type", tokenType)
                            .register(meterRegistry)
            );

            // 计数器增加指定数量
            counter.increment(tokenCount);

            log.debug("记录Token消耗: userId={}, appId={}, modelName={}, genMode={}, tokenType={}, tokenCount={}",
                    userId, appId, modelName, genMode, tokenType, tokenCount);
        } catch (Exception e) {
            // 捕获异常，避免监控逻辑影响业务流程
            log.error("记录Token消耗失败: userId={}, appId={}, modelName={}, genMode={}, tokenType={}, tokenCount={}",
                    userId, appId, modelName, genMode, tokenType, tokenCount, e);
        }
    }

    /**
     * 记录 AI 模型响应时间
     * 统计 AI 模型的响应时间，支持计算平均值、最大值、分位数等统计指标。
     *
     * @param userId    用户ID，用于按用户维度统计
     * @param appId     产物ID，用于按产物维度统计
     * @param modelName 模型名称
     * @param genMode   代码生成模式，"workflow" 或 "agent"
     * @param duration  响应时长，必须为非负数
     */
    public void recordResponseTime(String userId, String appId, String modelName, String genMode, Duration duration) {
        // 参数校验：确保所有参数都不为空，且响应时间为非负数
        if (userId == null || appId == null || modelName == null || genMode == null || duration == null) {
            log.warn("记录响应时间时参数不完整，跳过记录: userId={}, appId={}, modelName={}, genMode={}, duration={}",
                    userId, appId, modelName, genMode, duration);
            return;
        }

        if (duration.isNegative()) {
            log.warn("响应时间不能为负数，跳过记录: userId={}, appId={}, duration={}",
                    userId, appId, duration);
            return;
        }

        try {
            // 生成缓存键（响应时间不区分 token_type，所以维度较少）
            String cacheKey = String.format("%s_%s_%s_%s", userId, appId, modelName, genMode);

            // 从缓存中获取或创建 Timer
            Timer timer = responseTimersCache.computeIfAbsent(cacheKey, key ->
                    Timer.builder("ai_model_response_duration_seconds")
                            .description("AI模型响应时间")
                            .tag("user_id", userId)
                            .tag("app_id", appId)
                            .tag("model_name", modelName)
                            .tag("gen_mode", genMode)
                            .publishPercentileHistogram()
                            .serviceLevelObjectives(
                                    Duration.ofMillis(100),
                                    Duration.ofMillis(300),
                                    Duration.ofMillis(500),
                                    Duration.ofSeconds(1),
                                    Duration.ofSeconds(3),
                                    Duration.ofSeconds(5),
                                    Duration.ofSeconds(10),
                                    Duration.ofSeconds(30),
                                    Duration.ofSeconds(60)
                            )
                            .register(meterRegistry)
            );

            // 记录响应时间
            timer.record(duration);

            log.debug("记录响应时间: userId={}, appId={}, modelName={}, genMode={}, duration={}ms",
                    userId, appId, modelName, genMode, duration.toMillis());
        } catch (Exception e) {
            // 捕获异常，避免监控逻辑影响业务流程
            log.error("记录响应时间失败: userId={}, appId={}, modelName={}, genMode={}, duration={}",
                    userId, appId, modelName, genMode, duration, e);
        }
    }
}
