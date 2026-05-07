package com.rich.app.mq.producer;

import cn.hutool.core.lang.UUID;
import com.rich.app.mq.config.RabbitMQConfig;
import com.rich.app.mq.metrics.TaskMetrics;
import com.rich.app.mq.model.TaskMessage;
import com.rich.app.service.TaskExecutionService;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Vue项目构建任务生产者
 * 负责发送Vue项目构建消息到RabbitMQ队列
 *
 * @author DuRuiChi
 * @create 2026-05-06
 */
@Component
@Slf4j
public class VueProjectBuildProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private TaskExecutionService taskExecutionService;

    @Resource
    private TaskMetrics taskMetrics;

    private static final String BUILD_LOCK_PREFIX = "build:lock:";
    private static final String TASK_TYPE = "BUILD_VUE_PROJECT";

    /**
     * 发送Vue项目构建任务
     *
     * @param appId 产物ID
     * @param userId 用户ID
     * @param projectPath 项目路径
     * @param deployKey 部署密钥
     * @return 是否发送成功
     */
    public boolean sendBuildTask(Long appId, Long userId, String projectPath, String deployKey) {
        // 1. 幂等性检查(分布式锁)
        String lockKey = BUILD_LOCK_PREFIX + appId;
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofMinutes(10));

        if (Boolean.FALSE.equals(locked)) {
            log.warn("产物{}正在构建中,跳过重复任务", appId);
            return false;
        }

        try {
            // 2. 生成构建版本号
            String buildVersion = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

            // 3. 构建消息体
            TaskMessage message = TaskMessage.builder()
                    .messageId(UUID.randomUUID().toString(true))
                    .taskType(TASK_TYPE)
                    .appId(appId)
                    .userId(userId)
                    .payload(Map.of(
                            "projectPath", projectPath,
                            "buildVersion", buildVersion,
                            "deployKey", deployKey
                    ))
                    .retryCount(0)
                    .maxRetries(3)
                    .createTime(LocalDateTime.now())
                    .traceId(MDC.get("traceId"))
                    .build();

            // 4. 创建任务执行记录
            Long taskId = taskExecutionService.createTask(TASK_TYPE, appId, userId);
            if (taskId == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建任务记录失败");
            }

            // 5. 发送消息到RabbitMQ
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.BUILD_EXCHANGE,
                    RabbitMQConfig.BUILD_VUE_PROJECT_ROUTING_KEY,
                    message
            );

            // 6. 记录监控指标
            taskMetrics.recordTaskSubmit(TASK_TYPE);

            log.info("发送Vue构建任务成功: appId={}, messageId={}, projectPath={}", 
                    appId, message.getMessageId(), projectPath);

            return true;
        } catch (Exception e) {
            // 发送失败释放锁
            redisTemplate.delete(lockKey);
            log.error("发送Vue构建任务失败: appId={}, error={}", appId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "构建任务发送失败: " + e.getMessage());
        }
    }
}
