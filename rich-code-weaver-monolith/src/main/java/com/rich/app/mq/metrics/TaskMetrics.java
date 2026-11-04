package com.rich.app.mq.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 任务执行监控指标收集器
 *
 * @author DuRuiChi
 * @create 2026-05-06
 */
@Component
@Slf4j
public class TaskMetrics {

    private final MeterRegistry meterRegistry;

    // 任务提交计数器缓存
    private final ConcurrentHashMap<String, Counter> submitCounters = new ConcurrentHashMap<>();

    // 任务成功计数器缓存
    private final ConcurrentHashMap<String, Counter> successCounters = new ConcurrentHashMap<>();

    // 任务失败计数器缓存
    private final ConcurrentHashMap<String, Counter> failureCounters = new ConcurrentHashMap<>();

    // 任务执行耗时计时器缓存
    private final ConcurrentHashMap<String, Timer> durationTimers = new ConcurrentHashMap<>();

    public TaskMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        log.info("任务监控指标收集器初始化完成");
    }

    /**
     * 记录任务提交
     *
     * @param taskType 任务类型
     */
    public void recordTaskSubmit(String taskType) {
        Counter counter = submitCounters.computeIfAbsent(taskType, type ->
                Counter.builder("task.submit.total")
                        .description("任务提交总数")
                        .tag("task_type", type)
                        .register(meterRegistry)
        );
        counter.increment();
        log.debug("记录任务提交: taskType={}", taskType);
    }

    /**
     * 记录任务成功
     *
     * @param taskType 任务类型
     */
    public void recordTaskSuccess(String taskType) {
        Counter counter = successCounters.computeIfAbsent(taskType, type ->
                Counter.builder("task.success.total")
                        .description("任务成功总数")
                        .tag("task_type", type)
                        .register(meterRegistry)
        );
        counter.increment();
        log.debug("记录任务成功: taskType={}", taskType);
    }

    /**
     * 记录任务失败
     *
     * @param taskType 任务类型
     */
    public void recordTaskFailure(String taskType) {
        Counter counter = failureCounters.computeIfAbsent(taskType, type ->
                Counter.builder("task.failure.total")
                        .description("任务失败总数")
                        .tag("task_type", type)
                        .register(meterRegistry)
        );
        counter.increment();
        log.debug("记录任务失败: taskType={}", taskType);
    }

    /**
     * 记录任务执行耗时
     *
     * @param taskType 任务类型
     * @param durationMillis 耗时(毫秒)
     */
    public void recordTaskDuration(String taskType, long durationMillis) {
        Timer timer = durationTimers.computeIfAbsent(taskType, type ->
                Timer.builder("task.duration.seconds")
                        .description("任务执行耗时")
                        .tag("task_type", type)
                        .register(meterRegistry)
        );
        timer.record(durationMillis, TimeUnit.MILLISECONDS);
        log.debug("记录任务耗时: taskType={}, duration={}ms", taskType, durationMillis);
    }

    /**
     * 开始计时
     *
     * @return 开始时间戳(毫秒)
     */
    public long startTimer() {
        return System.currentTimeMillis();
    }

    /**
     * 结束计时并记录
     *
     * @param taskType 任务类型
     * @param startTime 开始时间戳
     */
    public void stopTimer(String taskType, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        recordTaskDuration(taskType, duration);
    }
}
