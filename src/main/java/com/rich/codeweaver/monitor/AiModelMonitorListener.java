package com.rich.codeweaver.monitor;

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
 * AI 模型监控监听器
 * <p>
 * 实现 LangChain4j 的 {@link ChatModelListener} 接口，监听 AI 模型调用的各个生命周期事件，
 * 并触发相应的指标收集操作。该监听器是整个监控体系的核心组件。
 * </p>
 */
@Component
@Slf4j
public class AiModelMonitorListener implements ChatModelListener {

    /**
     * 请求开始时间的存储键
     * <p>
     * 用于在请求上下文的 attributes 中存储请求开始时间，
     * 以便在响应或错误回调中计算响应时长。
     * </p>
     */
    private static final String REQUEST_START_TIME_KEY = "request_start_time";

    /**
     * 监控上下文的存储键
     */
    private static final String MONITOR_CONTEXT_KEY = "monitor_context";

    /**
     * AI 模型指标收集器
     */
    @Resource
    private AiModelMetricsCollector aiModelMetricsCollector;

    /**
     * AI 模型请求开始时的回调方法
     * <p>
     * 该方法在 AI 模型请求发送前被调用，主要完成以下任务：
     * <ul>
     *   <li>记录请求开始时间，用于后续计算响应时长</li>
     *   <li>从 ThreadLocal 获取监控上下文，并存储到请求属性中</li>
     *   <li>记录请求开始的指标（status=started）</li>
     * </ul>
     * </p>
     *
     * <p>线程切换问题：</p>
     * <ul>
     *   <li>onRequest 在主线程执行，可以直接从 ThreadLocal 获取上下文</li>
     *   <li>onResponse/onError 可能在其他线程执行，无法直接访问 ThreadLocal</li>
     *   <li>因此需要将上下文存储到 requestContext.attributes() 中传递</li>
     * </ul>
     *
     * @param requestContext AI 模型请求上下文，包含请求参数和属性存储
     */
    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        try {
            // 1. 记录请求开始时间（用于后续计算响应时长）
            requestContext.attributes().put(REQUEST_START_TIME_KEY, Instant.now());

            // 2. 从 ThreadLocal 获取监控上下文
            MonitorContext context = MonitorContextHolder.getContext();
            if (context == null) {
                log.warn("监控上下文为空，跳过请求监控。请确保在 AI 调用前设置了监控上下文。");
                return;
            }

            // 3. 将监控上下文存储到请求属性中（用于跨线程传递）
            // 因为 onResponse/onError 可能在不同线程执行，无法直接访问 ThreadLocal
            requestContext.attributes().put(MONITOR_CONTEXT_KEY, context);

            // 4. 提取监控维度信息
            String userId = context.getUserId();
            String appId = context.getAppId();
            String genMode = context.getGenMode();
            String modelName = requestContext.chatRequest().modelName();

            // 5. 记录请求开始指标
            aiModelMetricsCollector.recordRequest(userId, appId, modelName, genMode, "started");

            log.debug("AI模型请求开始: userId={}, appId={}, modelName={}, genMode={}",
                    userId, appId, modelName, genMode);
        } catch (Exception e) {
            // 捕获所有异常，避免监控逻辑影响业务流程
            log.error("处理AI模型请求开始事件时发生异常", e);
        }
    }

    /**
     * AI 模型响应成功时的回调方法
     * <p>
     * 该方法在 AI 模型成功返回响应后被调用，主要完成以下任务：
     * <ul>
     *   <li>从请求属性中获取监控上下文（因为可能在不同线程）</li>
     *   <li>记录请求成功的指标（status=success）</li>
     *   <li>记录响应时间（从请求开始到响应结束的时长）</li>
     *   <li>记录 Token 使用情况（input/output/total）</li>
     * </ul>
     * </p>
     *
     * <p>注意事项：</p>
     * <ul>
     *   <li>该方法可能在与 onRequest 不同的线程中执行</li>
     *   <li>必须从 attributes 中获取上下文，而不是 ThreadLocal</li>
     *   <li>Token 信息可能为 null，需要进行空值检查</li>
     * </ul>
     *
     * @param responseContext AI 模型响应上下文，包含响应数据和请求属性
     */
    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        try {
            // 1. 从请求属性中获取监控上下文（由 onRequest 方法存储）
            Map<Object, Object> attributes = responseContext.attributes();
            MonitorContext context = (MonitorContext) attributes.get(MONITOR_CONTEXT_KEY);

            if (context == null) {
                log.warn("监控上下文为空，跳过响应监控。可能是 onRequest 阶段未成功设置上下文。");
                return;
            }

            // 2. 提取监控维度信息
            String userId = context.getUserId();
            String appId = context.getAppId();
            String genMode = context.getGenMode();
            String modelName = responseContext.chatResponse().modelName();

            // 3. 记录请求成功指标
            aiModelMetricsCollector.recordRequest(userId, appId, modelName, genMode, "success");

            // 4. 记录响应时间
            recordResponseTime(attributes, userId, appId, modelName, genMode);

            // 5. 记录 Token 使用情况
            recordTokenUsage(responseContext, userId, appId, modelName, genMode);

            log.debug("AI模型响应成功: userId={}, appId={}, modelName={}, genMode={}",
                    userId, appId, modelName, genMode);
        } catch (Exception e) {
            // 捕获所有异常，避免监控逻辑影响业务流程
            log.error("处理AI模型响应成功事件时发生异常", e);
        }
    }

    /**
     * AI 模型调用出错时的回调方法
     * <p>
     * 该方法在 AI 模型调用失败时被调用，主要完成以下任务：
     * <ul>
     *   <li>从请求属性中获取监控上下文</li>
     *   <li>记录请求失败的指标（status=error）</li>
     *   <li>记录错误信息和错误次数</li>
     *   <li>记录响应时间（即使是错误响应也需要记录）</li>
     * </ul>
     * </p>
     *
     * <p>为什么要记录错误响应的时间：</p>
     * <ul>
     *   <li>分析错误请求的特征（如是否因为超时导致）</li>
     *   <li>对比成功请求和失败请求的响应时间差异</li>
     *   <li>识别性能瓶颈和系统问题</li>
     * </ul>
     *
     * @param errorContext AI 模型错误上下文，包含错误信息和请求属性
     */
    @Override
    public void onError(ChatModelErrorContext errorContext) {
        try {
            // 1. 从请求属性中获取监控上下文
            Map<Object, Object> attributes = errorContext.attributes();
            MonitorContext context = (MonitorContext) attributes.get(MONITOR_CONTEXT_KEY);

            if (context == null) {
                log.warn("监控上下文为空，跳过错误监控。可能是 onRequest 阶段未成功设置上下文。");
                return;
            }

            // 2. 提取监控维度信息
            String userId = context.getUserId();
            String appId = context.getAppId();
            String genMode = context.getGenMode();
            String modelName = errorContext.chatRequest().modelName();

            // 3. 提取错误信息（可能为 null）
            String errorMessage = errorContext.error() != null && errorContext.error().getMessage() != null
                    ? errorContext.error().getMessage()
                    : "Unknown error";

            // 4. 记录请求失败指标
            aiModelMetricsCollector.recordRequest(userId, appId, modelName, genMode, "error");

            // 5. 记录错误信息
            aiModelMetricsCollector.recordError(userId, appId, modelName, genMode, errorMessage);

            // 6. 记录响应时间（即使是错误响应）
            recordResponseTime(attributes, userId, appId, modelName, genMode);

            log.warn("AI模型调用失败: userId={}, appId={}, modelName={}, genMode={}, error={}",
                    userId, appId, modelName, genMode, errorMessage);
        } catch (Exception e) {
            // 捕获所有异常，避免监控逻辑影响业务流程
            log.error("处理AI模型错误事件时发生异常", e);
        }
    }

    /**
     * 记录响应时间的私有辅助方法
     * <p>
     * 从请求属性中获取开始时间，计算响应时长，并记录到监控系统。
     * 该方法被 onResponse 和 onError 共同调用，确保所有请求（无论成功或失败）
     * 的响应时间都被记录。
     * </p>
     *
     * <p>实现细节：</p>
     * <ul>
     *   <li>从 attributes 中获取请求开始时间（由 onRequest 存储）</li>
     *   <li>计算当前时间与开始时间的差值</li>
     *   <li>调用指标收集器记录响应时间</li>
     *   <li>如果开始时间不存在，记录警告但不抛出异常</li>
     * </ul>
     *
     * @param attributes 请求属性，包含开始时间等信息
     * @param userId     用户ID
     * @param appId      产物ID
     * @param modelName  模型名称
     * @param genMode    代码生成模式
     */
    private void recordResponseTime(Map<Object, Object> attributes, String userId, String appId,
                                     String modelName, String genMode) {
        try {
            // 1. 从属性中获取请求开始时间
            Instant startTime = (Instant) attributes.get(REQUEST_START_TIME_KEY);

            if (startTime == null) {
                log.warn("未找到请求开始时间，无法计算响应时长: userId={}, appId={}", userId, appId);
                return;
            }

            // 2. 计算响应时长
            Duration responseTime = Duration.between(startTime, Instant.now());

            // 3. 记录响应时间指标
            aiModelMetricsCollector.recordResponseTime(userId, appId, modelName, genMode, responseTime);

            log.debug("记录响应时间: userId={}, appId={}, modelName={}, genMode={}, duration={}ms",
                    userId, appId, modelName, genMode, responseTime.toMillis());
        } catch (Exception e) {
            // 捕获异常，避免影响主流程
            log.error("记录响应时间时发生异常: userId={}, appId={}", userId, appId, e);
        }
    }

    /**
     * 记录 Token 使用情况的私有辅助方法
     * <p>
     * 从响应上下文中提取 Token 使用信息，并分别记录输入、输出和总计 Token 数量。
     * 该方法仅在 onResponse 中调用，因为只有成功的响应才包含 Token 信息。
     * </p>
     *
     * <p>Token 类型说明：</p>
     * <ul>
     *   <li>input: 输入 Token 数量，即用户提示词消耗的 Token</li>
     *   <li>output: 输出 Token 数量，即 AI 生成内容消耗的 Token</li>
     *   <li>total: 总 Token 数量，通常等于 input + output</li>
     * </ul>
     *
     * <p>注意事项：</p>
     * <ul>
     *   <li>Token 信息可能为 null，需要进行空值检查</li>
     *   <li>不同模型的 Token 计算方式可能不同</li>
     *   <li>某些模型可能不提供详细的 Token 统计</li>
     * </ul>
     *
     * @param responseContext 响应上下文，包含 Token 使用信息
     * @param userId          用户ID
     * @param appId           产物ID
     * @param modelName       模型名称
     * @param genMode         代码生成模式
     */
    private void recordTokenUsage(ChatModelResponseContext responseContext, String userId, String appId,
                                   String modelName, String genMode) {
        try {
            // 1. 从响应中提取 Token 使用信息
            TokenUsage tokenUsage = responseContext.chatResponse().metadata().tokenUsage();

            if (tokenUsage == null) {
                log.debug("响应中未包含Token使用信息: userId={}, appId={}", userId, appId);
                return;
            }

            // 2. 记录输入 Token 数量
            if (tokenUsage.inputTokenCount() != null && tokenUsage.inputTokenCount() > 0) {
                aiModelMetricsCollector.recordTokenUsage(
                        userId, appId, modelName, genMode, "input", tokenUsage.inputTokenCount());
            }

            // 3. 记录输出 Token 数量
            if (tokenUsage.outputTokenCount() != null && tokenUsage.outputTokenCount() > 0) {
                aiModelMetricsCollector.recordTokenUsage(
                        userId, appId, modelName, genMode, "output", tokenUsage.outputTokenCount());
            }

            // 4. 记录总 Token 数量
            if (tokenUsage.totalTokenCount() != null && tokenUsage.totalTokenCount() > 0) {
                aiModelMetricsCollector.recordTokenUsage(
                        userId, appId, modelName, genMode, "total", tokenUsage.totalTokenCount());
            }

            log.debug("记录Token使用情况: userId={}, appId={}, modelName={}, genMode={}, " +
                            "input={}, output={}, total={}",
                    userId, appId, modelName, genMode,
                    tokenUsage.inputTokenCount(), tokenUsage.outputTokenCount(), tokenUsage.totalTokenCount());
        } catch (Exception e) {
            // 捕获异常，避免影响主流程
            log.error("记录Token使用情况时发生异常: userId={}, appId={}", userId, appId, e);
        }
    }
}
