package com.rich.richcodeweaver.sysMonitor;

import com.rich.richcodeweaver.sysMonitor.context.SysMonitorContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 系统监控上下文持有者，用于分配给线程保存监控上下文信息
 *
 * @author DuRuiChi
 * @create 2025/11/8
 **/
@Slf4j
public class SysMonitorContextHolder {

    private static final ThreadLocal<SysMonitorContext> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置监控上下文
     */
    public static void setContext(SysMonitorContext context) {
        // 为当前线程分配 ThreadLocal
        CONTEXT_HOLDER.set(context);
    }

    /**
     * 获取当前监控上下文
     */
    public static SysMonitorContext getContext() {
        // 获取前线程分配 ThreadLocal
        return CONTEXT_HOLDER.get();
    }

    /**
     * 清除监控上下文
     */
    public static void clearContext() {
        // 移除当前线程分配的 ThreadLocal
        CONTEXT_HOLDER.remove();
    }
}
