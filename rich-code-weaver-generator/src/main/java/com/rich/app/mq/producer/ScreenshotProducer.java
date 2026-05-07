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
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 产物截图生成任务生产者
 * 负责发送截图生成消息到RabbitMQ队列
 *
 * @author DuRuiChi
 * @create 2026-05-06
 */
@Component
@Slf4j
public class ScreenshotProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private TaskExecutionService taskExecutionService;

    @Resource
    private TaskMetrics taskMetrics;

    private static final String TASK_TYPE = "GENERATE_SCREENSHOT";

    /**
     * 发送截图生成任务
     *
     * @param appId 产物ID
     * @param appUrl 产物访问URL
     * @param screenshotType 截图类型(COVER-封面图)
     * @return 是否发送成功
     */
    public boolean sendScreenshotTask(Long appId, String appUrl, String screenshotType) {
        try {
            // 1. 构建消息体
            TaskMessage message = TaskMessage.builder()
                    .messageId(UUID.randomUUID().toString(true))
                    .taskType(TASK_TYPE)
                    .appId(appId)
                    .userId(null) // 截图任务不需要用户ID
                    .payload(Map.of(
                            "appUrl", appUrl,
                            "screenshotType", screenshotType
                    ))
                    .retryCount(0)
                    .maxRetries(3)
                    .createTime(LocalDateTime.now())
                    .traceId(MDC.get("traceId"))
                    .build();

            // 2. 创建任务执行记录
            Long taskId = taskExecutionService.createTask(TASK_TYPE, appId, null);
            if (taskId == null) {
                log.warn("创建截图任务记录失败，但继续发送消息: appId={}", appId);
            }

            // 3. 发送消息到RabbitMQ
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SCREENSHOT_EXCHANGE,
                    RabbitMQConfig.GENERATE_SCREENSHOT_ROUTING_KEY,
                    message
            );

            // 4. 记录监控指标
            taskMetrics.recordTaskSubmit(TASK_TYPE);

            log.info("发送截图生成任务成功: appId={}, messageId={}, appUrl={}", 
                    appId, message.getMessageId(), appUrl);
            return true;

        } catch (Exception e) {
            log.error("发送截图生成任务失败: appId={}, error={}", appId, e.getMessage(), e);
            // 截图任务失败不影响主流程，只记录日志
            return false;
        }
    }
}
