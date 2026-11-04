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

        boolean saved = this.save(taskExecution);
        if (saved) {
            // 同步到Redis缓存
            cacheTaskStatus(appId, "PENDING", 0, null);
            log.info("创建任务执行记录成功: taskType={}, appId={}, userId={}", taskType, appId, userId);
            return taskExecution.getId();
        }
        
        log.error("创建任务执行记录失败: taskType={}, appId={}, userId={}", taskType, appId, userId);
        return null;
    }

    @Override
    public void updateStatus(Long appId, String status, Integer progress) {
        TaskExecution latestTask = getLatestByAppId(appId);
        if (latestTask == null) {
            log.warn("未找到产物{}的任务执行记录", appId);
            return;
        }

        TaskExecution updateTask = new TaskExecution();
        updateTask.setId(latestTask.getId());
        updateTask.setStatus(status);
        updateTask.setProgress(progress);
        updateTask.setUpdateTime(LocalDateTime.now());

        // 如果是开始执行，记录开始时间
        if ("RUNNING".equals(status) && latestTask.getStartTime() == null) {
            updateTask.setStartTime(LocalDateTime.now());
        }

        // 如果是完成状态，记录结束时间
        if ("SUCCESS".equals(status) || "FAILED".equals(status)) {
            updateTask.setEndTime(LocalDateTime.now());
        }

        boolean updated = this.updateById(updateTask);
        if (updated) {
            // 同步到Redis缓存
            cacheTaskStatus(appId, status, progress, null);
            log.debug("更新任务状态成功: appId={}, status={}, progress={}", appId, status, progress);
        }
    }

    @Override
    public void updateResult(Long appId, String result) {
        TaskExecution latestTask = getLatestByAppId(appId);
        if (latestTask == null) {
            log.warn("未找到产物{}的任务执行记录", appId);
            return;
        }

        TaskExecution updateTask = new TaskExecution();
        updateTask.setId(latestTask.getId());
        updateTask.setResult(result);
        updateTask.setUpdateTime(LocalDateTime.now());

        boolean updated = this.updateById(updateTask);
        if (updated) {
            // 同步到Redis缓存
            cacheTaskStatus(appId, latestTask.getStatus(), latestTask.getProgress(), result);
            log.debug("更新任务结果成功: appId={}, result={}", appId, 
                    StrUtil.sub(result, 0, 100)); // 只记录前100字符
        }
    }

    @Override
    public TaskExecution getLatestByAppId(Long appId) {
        // 先从Redis缓存查询
        String cacheKey = TASK_STATUS_KEY_PREFIX + appId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("从Redis缓存获取任务状态: appId={}", appId);
        }

        // 从数据库查询最新记录
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where("app_id = ?", appId)
                .orderBy("create_time", false)
                .limit(1);

        return this.getOne(queryWrapper);
    }

    /**
     * 缓存任务状态到Redis
     */
    private void cacheTaskStatus(Long appId, String status, Integer progress, String result) {
        String cacheKey = TASK_STATUS_KEY_PREFIX + appId;
        
        TaskStatusCache cache = TaskStatusCache.builder()
                .status(status)
                .progress(progress)
                .message(result)
                .updateTime(System.currentTimeMillis())
                .build();

        redisTemplate.opsForValue().set(cacheKey, cache, TASK_STATUS_TTL);
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
