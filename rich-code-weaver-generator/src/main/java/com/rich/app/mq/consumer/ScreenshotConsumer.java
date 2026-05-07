package com.rich.app.mq.consumer;

import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.Channel;
import com.rich.app.mq.config.RabbitMQConfig;
import com.rich.app.mq.metrics.TaskMetrics;
import com.rich.app.mq.model.TaskMessage;
import com.rich.app.mq.notifier.WebSocketTaskNotifier;
import com.rich.app.service.AppService;
import com.rich.app.service.TaskExecutionService;
import com.rich.client.innerService.InnerScreenshotService;
import com.rich.common.constant.AppConstant;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.model.entity.App;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 产物截图生成任务消费者
 * 负责消费截图生成消息并执行截图任务
 *
 * @author DuRuiChi
 * @create 2026-05-06
 */
@Component
@Slf4j
public class ScreenshotConsumer {

    @DubboReference(check = false)
    private InnerScreenshotService screenshotService;

    @Resource
    private AppService appService;

    @Resource
    private TaskExecutionService taskExecutionService;

    @Resource
    private WebSocketTaskNotifier webSocketNotifier;

    @Resource
    private TaskMetrics taskMetrics;

    @Resource
    private RabbitTemplate rabbitTemplate;

    private static final String TASK_TYPE = "GENERATE_SCREENSHOT";
    private static final long RETRY_INITIAL_INTERVAL_MS = 3000L;

    /**
     * 消费截图生成任务
     */
    @RabbitListener(queues = RabbitMQConfig.GENERATE_SCREENSHOT_QUEUE)
    public void consumeScreenshotTask(TaskMessage message, Channel channel, Message amqpMessage,
                                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        long startTime = taskMetrics.startTimer();

        try {
            log.info("开始处理截图生成任务: appId={}, messageId={}", 
                    message.getAppId(), message.getMessageId());

            // 1. 更新任务状态为执行中
            taskExecutionService.updateStatus(message.getAppId(), "RUNNING", 0);
            webSocketNotifier.notifyProgress(message.getAppId(), "RUNNING", 0, "开始生成截图");

            // 2. 获取产物URL
            String appUrl = (String) message.getPayload().get("appUrl");
            if (StrUtil.isBlank(appUrl)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "产物URL为空");
            }

            // 3. 调用截图服务生成截图
            log.info("调用截图服务: appId={}, appUrl={}", message.getAppId(), appUrl);
            taskExecutionService.updateStatus(message.getAppId(), "RUNNING", 50);
            webSocketNotifier.notifyProgress(message.getAppId(), "RUNNING", 50, "正在截图中");

            String screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);

            // 4. 校验截图URL
            if (StrUtil.isBlank(screenshotUrl)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "截图URL为空");
            }

            log.info("截图生成成功: appId={}, screenshotUrl={}", message.getAppId(), screenshotUrl);

            // 5. 更新产物封面
            App updateApp = new App();
            updateApp.setId(message.getAppId());
            updateApp.setCover(screenshotUrl);

            boolean updated = appService.updateById(updateApp);
            if (!updated) {
                log.warn("更新产物封面失败: appId={}", message.getAppId());
            }

            // 6. 更新任务状态为成功
            taskExecutionService.updateStatus(message.getAppId(), "SUCCESS", 100);
            taskExecutionService.updateResult(message.getAppId(), screenshotUrl);
            webSocketNotifier.notifySuccess(message.getAppId(), screenshotUrl);

            // 7. 手动ACK
            channel.basicAck(deliveryTag, false);

            // 8. 记录监控指标
            taskMetrics.recordTaskSuccess(TASK_TYPE);
            taskMetrics.stopTimer(TASK_TYPE, startTime);

            log.info("截图生成任务完成: appId={}, screenshotUrl={}", message.getAppId(), screenshotUrl);

        } catch (Exception e) {
            log.error("截图生成任务失败: appId={}, error={}", message.getAppId(), e.getMessage(), e);
            
            // 失败处理
            handleScreenshotFailure(message, channel, deliveryTag, e, startTime);
        }
    }

    /**
     * 处理截图失败
     */
    private void handleScreenshotFailure(TaskMessage message, Channel channel, 
                                          long deliveryTag, Exception e, long startTime) {
        try {
            int retryCount = message.getRetryCount();
            int maxRetries = message.getMaxRetries();

            if (retryCount < maxRetries) {
                // 指数退避: 3s, 6s, 12s
                long backoffMs = RETRY_INITIAL_INTERVAL_MS * (1L << retryCount);
                log.warn("截图失败,准备重试: appId={}, retryCount={}/{}, backoff={}ms", 
                        message.getAppId(), retryCount + 1, maxRetries, backoffMs);

                message.setRetryCount(retryCount + 1);
                taskExecutionService.updateStatus(message.getAppId(), "RETRYING", 0);
                webSocketNotifier.notifyRetrying(message.getAppId(), retryCount + 1, maxRetries);

                // 指数退避等待
                Thread.sleep(backoffMs);

                // 重新发布带有新retryCount的消息
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.SCREENSHOT_EXCHANGE,
                        RabbitMQConfig.GENERATE_SCREENSHOT_ROUTING_KEY,
                        message
                );

                // ACK原始消息
                channel.basicAck(deliveryTag, false);

            } else {
                // 超过最大重试次数,使用默认封面
                log.error("截图失败且已达最大重试次数,使用默认封面: appId={}", message.getAppId());

                // 使用默认封面
                App updateApp = new App();
                updateApp.setId(message.getAppId());
                updateApp.setCover(AppConstant.APP_COVER);
                appService.updateById(updateApp);

                String errorMessage = e.getMessage() != null ? e.getMessage() : "截图生成失败";
                taskExecutionService.updateStatus(message.getAppId(), "FAILED", 0);
                taskExecutionService.updateResult(message.getAppId(), "使用默认封面: " + errorMessage);
                
                // 不推送失败通知，避免影响用户体验
                log.info("截图失败已降级处理: appId={}", message.getAppId());

                // 确认消费,不再重试
                channel.basicAck(deliveryTag, false);

                // 记录监控指标
                taskMetrics.recordTaskFailure(TASK_TYPE);
                taskMetrics.stopTimer(TASK_TYPE, startTime);
            }
        } catch (Exception ex) {
            log.error("重试处理失败: appId={}", message.getAppId(), ex);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException nackEx) {
                log.error("NACK失败: appId={}", message.getAppId(), nackEx);
            }
        }
    }
}
