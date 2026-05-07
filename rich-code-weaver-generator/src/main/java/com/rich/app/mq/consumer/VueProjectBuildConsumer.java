package com.rich.app.mq.consumer;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.Channel;
import com.rich.app.mq.config.RabbitMQConfig;
import com.rich.app.mq.metrics.TaskMetrics;
import com.rich.app.mq.model.TaskMessage;
import com.rich.app.mq.notifier.WebSocketTaskNotifier;
import com.rich.app.mq.producer.ScreenshotProducer;
import com.rich.app.service.AppService;
import com.rich.app.service.TaskExecutionService;
import com.rich.common.constant.AppConstant;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.common.utils.deployWebProjectUtils.BuildWebProjectExecutor;
import com.rich.common.utils.deployWebProjectUtils.ExecuteSysCommandUtil;
import com.rich.model.entity.App;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Vue项目构建任务消费者
 * 负责消费Vue项目构建消息并执行构建任务
 *
 * @author DuRuiChi
 * @create 2026-05-06
 */
@Component
@Slf4j
public class VueProjectBuildConsumer {

    @Resource
    private BuildWebProjectExecutor buildExecutor;

    @Resource
    private TaskExecutionService taskExecutionService;

    @Resource
    private WebSocketTaskNotifier webSocketNotifier;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private TaskMetrics taskMetrics;

    @Resource
    private AppService appService;

    @Resource
    private ScreenshotProducer screenshotProducer;

    private static final String BUILD_LOCK_PREFIX = "build:lock:";
    private static final String TASK_TYPE = "BUILD_VUE_PROJECT";
    private static final long RETRY_INITIAL_INTERVAL_MS = 3000L;

    /**
     * 消费Vue项目构建任务
     */
    @RabbitListener(queues = RabbitMQConfig.BUILD_VUE_PROJECT_QUEUE)
    public void consumeBuildTask(TaskMessage message, Channel channel, Message amqpMessage,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        String lockKey = BUILD_LOCK_PREFIX + message.getAppId();
        long startTime = taskMetrics.startTimer();

        try {
            log.info("开始处理Vue构建任务: appId={}, messageId={}", 
                    message.getAppId(), message.getMessageId());

            // 1. 更新任务状态为执行中
            taskExecutionService.updateStatus(message.getAppId(), "RUNNING", 0);
            webSocketNotifier.notifyProgress(message.getAppId(), "RUNNING", 0, "开始构建");

            // 2. 获取项目路径
            String projectPath = (String) message.getPayload().get("projectPath");
            if (StrUtil.isBlank(projectPath)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "项目路径为空");
            }

            File projectDir = new File(projectPath);
            if (!projectDir.exists() || !projectDir.isDirectory()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "项目目录不存在: " + projectPath);
            }

            // 3. 执行npm install
            log.info("开始安装依赖: appId={}, projectPath={}", message.getAppId(), projectPath);
            taskExecutionService.updateStatus(message.getAppId(), "RUNNING", 30);
            webSocketNotifier.notifyProgress(message.getAppId(), "RUNNING", 30, "安装依赖中");

            String npmInstallCmd = System.getProperty("os.name").toLowerCase().contains("win") 
                    ? "npm.cmd install" : "npm install";
            
            ExecuteSysCommandUtil.CommandResult installResult = 
                    ExecuteSysCommandUtil.executeCommandWithLog(projectDir, npmInstallCmd, 300);

            if (!installResult.success()) {
                String errorLog = StrUtil.sub(installResult.log(), 0, 500);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                        "依赖安装失败: " + errorLog);
            }

            log.info("依赖安装完成: appId={}", message.getAppId());

            // 4. 执行npm run build
            log.info("开始构建项目: appId={}, projectPath={}", message.getAppId(), projectPath);
            taskExecutionService.updateStatus(message.getAppId(), "RUNNING", 60);
            webSocketNotifier.notifyProgress(message.getAppId(), "RUNNING", 60, "构建项目中");

            String npmBuildCmd = System.getProperty("os.name").toLowerCase().contains("win") 
                    ? "npm.cmd run build" : "npm run build";
            
            ExecuteSysCommandUtil.CommandResult buildResult = 
                    ExecuteSysCommandUtil.executeCommandWithLog(projectDir, npmBuildCmd, 300);

            if (!buildResult.success()) {
                String errorLog = StrUtil.sub(buildResult.log(), 0, 500);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                        "项目构建失败: " + errorLog);
            }

            log.info("项目构建完成: appId={}", message.getAppId());

            // 5. 验证dist目录
            File distDir = new File(projectDir, "dist");
            if (!distDir.exists() || !distDir.isDirectory()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                        "dist目录未生成，请检查vite.config.js的outDir配置");
            }

            // 6. 部署：复制dist到部署目录
            taskExecutionService.updateStatus(message.getAppId(), "RUNNING", 80);
            webSocketNotifier.notifyProgress(message.getAppId(), "RUNNING", 80, "正在部署");

            String deployKey = (String) message.getPayload().get("deployKey");
            String appUrl = null;
            if (StrUtil.isNotBlank(deployKey)) {
                File deployDir = new File(AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey);
                FileUtil.copyContent(distDir, deployDir, true);
                log.info("dist目录已复制到部署目录: appId={}, deployDir={}", message.getAppId(), deployDir.getAbsolutePath());

                // 7. 更新产物信息
                App updateApp = new App();
                updateApp.setId(message.getAppId());
                updateApp.setDeployedTime(LocalDateTime.now());
                appService.updateById(updateApp);

                appUrl = String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);

                // 8. 异步生成截图
                screenshotProducer.sendScreenshotTask(message.getAppId(), appUrl, "COVER");
            }

            // 9. 更新任务状态为成功
            String result = appUrl != null ? appUrl : distDir.getAbsolutePath();
            taskExecutionService.updateStatus(message.getAppId(), "SUCCESS", 100);
            taskExecutionService.updateResult(message.getAppId(), result);
            webSocketNotifier.notifySuccess(message.getAppId(), result);

            // 10. 手动ACK
            channel.basicAck(deliveryTag, false);

            // 11. 记录监控指标
            taskMetrics.recordTaskSuccess(TASK_TYPE);
            taskMetrics.stopTimer(TASK_TYPE, startTime);

            log.info("Vue构建部署任务完成: appId={}, result={}", message.getAppId(), result);

        } catch (Exception e) {
            log.error("Vue构建任务失败: appId={}, error={}", message.getAppId(), e.getMessage(), e);
            
            // 失败处理
            handleBuildFailure(message, channel, deliveryTag, e, startTime);
        } finally {
            // 释放分布式锁
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 处理构建失败
     */
    private void handleBuildFailure(TaskMessage message, Channel channel, 
                                     long deliveryTag, Exception e, long startTime) {
        try {
            int retryCount = message.getRetryCount();
            int maxRetries = message.getMaxRetries();

            if (retryCount < maxRetries) {
                // 指数退避: 3s, 6s, 12s
                long backoffMs = RETRY_INITIAL_INTERVAL_MS * (1L << retryCount);
                log.warn("构建失败,准备重试: appId={}, retryCount={}/{}, backoff={}ms", 
                        message.getAppId(), retryCount + 1, maxRetries, backoffMs);

                message.setRetryCount(retryCount + 1);
                taskExecutionService.updateStatus(message.getAppId(), "RETRYING", 0);
                webSocketNotifier.notifyRetrying(message.getAppId(), retryCount + 1, maxRetries);

                // 指数退避等待
                Thread.sleep(backoffMs);

                // 重新发布带有新retryCount的消息（解决NACK+requeue不持久化retryCount的问题）
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.BUILD_EXCHANGE,
                        RabbitMQConfig.BUILD_VUE_PROJECT_ROUTING_KEY,
                        message
                );

                // ACK原始消息
                channel.basicAck(deliveryTag, false);

            } else {
                // 超过最大重试次数,标记为失败
                log.error("构建失败且已达最大重试次数: appId={}", message.getAppId());

                String errorMessage = e.getMessage() != null ? e.getMessage() : "未知错误";
                taskExecutionService.updateStatus(message.getAppId(), "FAILED", 0);
                taskExecutionService.updateResult(message.getAppId(), errorMessage);
                webSocketNotifier.notifyFailure(message.getAppId(), errorMessage);

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
