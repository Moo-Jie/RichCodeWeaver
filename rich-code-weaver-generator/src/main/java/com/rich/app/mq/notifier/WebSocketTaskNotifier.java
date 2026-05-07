package com.rich.app.mq.notifier;

import com.rich.app.mq.model.TaskProgressMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * WebSocket任务进度通知器
 * 用于向前端推送任务进度更新
 *
 * @author DuRuiChi
 * @create 2026-05-06
 */
@Component
@Slf4j
public class WebSocketTaskNotifier {

    @Resource
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 推送任务进度
     *
     * @param appId 产物ID
     * @param status 任务状态
     * @param progress 任务进度(0-100)
     * @param message 进度描述信息
     */
    public void notifyProgress(Long appId, String status, Integer progress, String message) {
        notifyProgress(appId, status, progress, message, null);
    }

    /**
     * 推送任务进度(带结果)
     *
     * @param appId 产物ID
     * @param status 任务状态
     * @param progress 任务进度(0-100)
     * @param message 进度描述信息
     * @param result 任务结果
     */
    public void notifyProgress(Long appId, String status, Integer progress, String message, String result) {
        try {
            TaskProgressMessage progressMessage = TaskProgressMessage.builder()
                    .appId(appId)
                    .status(status)
                    .progress(progress)
                    .message(message)
                    .result(result)
                    .timestamp(System.currentTimeMillis())
                    .build();

            // 推送到指定产物的订阅者
            String destination = "/topic/task/progress/" + appId;
            messagingTemplate.convertAndSend(destination, progressMessage);

            log.debug("推送任务进度成功: appId={}, status={}, progress={}%, message={}", 
                    appId, status, progress, message);
        } catch (Exception e) {
            log.error("推送任务进度失败: appId={}, error={}", appId, e.getMessage(), e);
        }
    }

    /**
     * 推送任务成功通知
     *
     * @param appId 产物ID
     * @param result 任务结果
     */
    public void notifySuccess(Long appId, String result) {
        notifyProgress(appId, "SUCCESS", 100, "任务完成", result);
    }

    /**
     * 推送任务失败通知
     *
     * @param appId 产物ID
     * @param errorMessage 错误信息
     */
    public void notifyFailure(Long appId, String errorMessage) {
        notifyProgress(appId, "FAILED", 0, "任务失败", errorMessage);
    }

    /**
     * 推送任务重试通知
     *
     * @param appId 产物ID
     * @param retryCount 当前重试次数
     * @param maxRetries 最大重试次数
     */
    public void notifyRetrying(Long appId, int retryCount, int maxRetries) {
        String message = String.format("任务失败，正在重试 (%d/%d)", retryCount, maxRetries);
        notifyProgress(appId, "RETRYING", 0, message);
    }
}
