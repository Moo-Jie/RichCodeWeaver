package com.rich.ai.monitor;

import lombok.extern.slf4j.Slf4j;

/**
 * AI 模型监控上下文存储器
 * 用于在 AI 模型调用的整个生命周期中，通过 ThreadLocal 在线程间传递 AI 模型监控上下文。
 *
 * @author DuRuiChi
 * @create 2026/03/31
 * @see MonitorContext
 * @see AiModelMonitorListener
 **/
@Slf4j
public class MonitorContextHolder {

    /**
     * ThreadLocal 存储，为每个线程维护独立的监控上下文
     */
    private static final ThreadLocal<MonitorContext> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前线程的监控上下文
     * 该方法应在 AI 模型调用前执行，将业务维度信息（用户ID、产物ID、生成模式等）存储到 ThreadLocal 中，供监听器在后续的回调方法中获取。
     *
     * @param context 监控上下文对象，包含用户ID、产物ID、生成模式等信息
     *                如果传入 null，将清空当前线程的上下文
     * @see #getContext()
     * @see #clearContext()
     */
    public static void setContext(MonitorContext context) {
        if (context == null) {
            log.warn("尝试设置 null 监控上下文，将清空当前线程的上下文");
            CONTEXT_HOLDER.remove();
            return;
        }

        CONTEXT_HOLDER.set(context);
        log.debug("设置监控上下文: userId={}, appId={}, genMode={}",
                context.getUserId(), context.getAppId(), context.getGenMode());
    }

    /**
     * 获取当前线程的监控上下文
     * 该方法通常在 AI 模型监听器的回调方法中调用，用于获取业务维度信息。
     *
     * @return 当前线程的监控上下文，如果未设置则返回 null
     * @see #setContext(MonitorContext)
     */
    public static MonitorContext getContext() {
        MonitorContext context = CONTEXT_HOLDER.get();
        if (context == null) {
            log.debug("当前线程未设置监控上下文");
        }
        return context;
    }

    /**
     * 清除当前线程的监控上下文
     * 该方法必须在 AI 模型调用结束后执行，用于清理 ThreadLocal 中的上下文对象，
     *
     * @see #setContext(MonitorContext)
     */
    public static void clearContext() {
        MonitorContext context = CONTEXT_HOLDER.get();
        if (context != null) {
            log.debug("清除监控上下文: userId={}, appId={}, genMode={}",
                    context.getUserId(), context.getAppId(), context.getGenMode());
        }
        CONTEXT_HOLDER.remove();
    }
}
