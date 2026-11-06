package com.rich.app.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.app.mapper.TaskExecutionMapper;
import com.rich.app.service.TaskExecutionService;
import com.rich.model.entity.TaskExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 任务执行记录服务实现(Mysql + Redis 双写)
 *
 * @author DuRuiChi
 * @create 2026-05-06
 */
@Service
@Slf4j
public class TaskExecutionServiceImpl extends ServiceImpl<TaskExecutionMapper, TaskExecution>
        implements TaskExecutionService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TASK_STATUS_KEY_PREFIX = "task:status:";
    private static final Duration TASK_STATUS_TTL = Duration.ofHours(1);

    /**
     * 任务表缺失时只完整记录一次告警，避免构建过程中重复刷屏
     */
    private final AtomicBoolean databaseWarningLogged = new AtomicBoolean(false);

    /**
     * 创建任务执行记录，数据库不可用时退化为 Redis 状态记录
     *
     * @param taskType 任务类型
     * @param appId 产物ID
     * @param userId 用户ID
     * @return 任务执行记录ID
     */
    @Override
    public Long createTask(String taskType, Long appId, Long userId) {
        TaskExecution taskExecution = TaskExecution.builder()
                .taskType(taskType)
                .appId(appId)
                .userId(userId)
                .status("PENDING")
                .progress(0)
                .retryCount(0)
                .maxRetries(3)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        try {
            boolean saved = this.save(taskExecution);
            if (saved) {
                cacheTaskStatus(appId, "PENDING", 0, null);
                log.info("创建任务执行记录成功: taskType={}, appId={}, userId={}", taskType, appId, userId);
                return taskExecution.getId();
            }
            log.error("创建任务执行记录失败: taskType={}, appId={}, userId={}", taskType, appId, userId);
        } catch (Exception e) {
            // 任务表可能尚未迁移，任务状态仍写入 Redis，避免阻断 RabbitMQ 构建和部署链路
            log.error("创建任务执行记录异常，已降级为 Redis 状态: taskType={}, appId={}", taskType, appId, e);
        }

        cacheTaskStatus(appId, "PENDING", 0, null);
        return appId == null ? -System.currentTimeMillis() : -Math.abs(appId);
    }

    /**
     * 更新任务状态和进度，Redis 始终更新，数据库更新为尽力而为
     *
     * @param appId 产物ID
     * @param status 任务状态
     * @param progress 任务进度
     */
    @Override
    public void updateStatus(Long appId, String status, Integer progress) {
        cacheTaskStatus(appId, status, progress, null);
        TaskExecution latestTask = getLatestByAppId(appId);
        if (latestTask == null) {
            log.debug("未找到产物{}的任务数据库记录，状态已写入 Redis", appId);
            return;
        }

        TaskExecution updateTask = new TaskExecution();
        updateTask.setId(latestTask.getId());
        updateTask.setStatus(status);
        updateTask.setProgress(progress);
        updateTask.setUpdateTime(LocalDateTime.now());
        if ("RUNNING".equals(status) && latestTask.getStartTime() == null) {
            updateTask.setStartTime(LocalDateTime.now());
        }
        if ("SUCCESS".equals(status) || "FAILED".equals(status)) {
            updateTask.setEndTime(LocalDateTime.now());
        }

        try {
            boolean updated = this.updateById(updateTask);
            if (updated) {
                log.debug("更新任务状态成功: appId={}, status={}, progress={}", appId, status, progress);
            }
        } catch (Exception e) {
            log.warn("更新任务数据库状态失败，Redis 状态不受影响: appId={}, status={}", appId, status, e);
        }
    }

    /**
     * 更新任务结果，数据库不可用时仍保留结构化日志给调用链
     *
     * @param appId 产物ID
     * @param result 任务结果
     */
    @Override
    public void updateResult(Long appId, String result) {
        TaskExecution latestTask = getLatestByAppId(appId);
        if (latestTask == null) {
            log.debug("未找到产物{}的任务数据库记录，结果未落库: {}", appId, StrUtil.sub(result, 0, 100));
            return;
        }

        TaskExecution updateTask = new TaskExecution();
        updateTask.setId(latestTask.getId());
        updateTask.setResult(result);
        updateTask.setUpdateTime(LocalDateTime.now());

        try {
            boolean updated = this.updateById(updateTask);
            if (updated) {
                log.debug("更新任务结果成功: appId={}, result={}", appId, StrUtil.sub(result, 0, 100));
            }
        } catch (Exception e) {
            log.warn("更新任务数据库结果失败: appId={}", appId, e);
        }
    }

    /**
     * 根据产物ID查询最新的任务执行记录
     *
     * @param appId 产物ID
     * @return 任务执行记录，数据库不可用时返回 null
     */
    @Override
    public TaskExecution getLatestByAppId(Long appId) {
        if (appId == null) {
            return null;
        }
        try {
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .where("app_id = ?", appId)
                    .orderBy("create_time", false)
                    .limit(1);
            TaskExecution taskExecution = this.getOne(queryWrapper);
            databaseWarningLogged.set(false);
            return taskExecution;
        } catch (Exception e) {
            if (databaseWarningLogged.compareAndSet(false, true)) {
                log.warn("查询任务数据库记录失败，后续继续使用 Redis 状态: appId={}", appId, e);
            } else {
                log.debug("任务数据库仍不可用，继续使用 Redis 状态: appId={}, error={}", appId, e.getMessage());
            }
            return null;
        }
    }

    /**
     * 缓存任务状态到 Redis
     *
     * @param appId 产物ID
     * @param status 状态
     * @param progress 进度
     * @param result 结果
     */
    private void cacheTaskStatus(Long appId, String status, Integer progress, String result) {
        if (appId == null) {
            return;
        }
        String cacheKey = TASK_STATUS_KEY_PREFIX + appId;
        try {
            TaskStatusCache cache = TaskStatusCache.builder()
                    .status(status)
                    .progress(progress)
                    .message(result)
                    .updateTime(System.currentTimeMillis())
                    .build();
            redisTemplate.opsForValue().set(cacheKey, cache, TASK_STATUS_TTL);
        } catch (Exception e) {
            log.warn("写入任务状态缓存失败: appId={}, status={}", appId, status, e);
        }
    }

    /**
     * 任务状态缓存对象
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class TaskStatusCache implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private String status;
        private Integer progress;
        private String message;
        private Long updateTime;
    }
}
