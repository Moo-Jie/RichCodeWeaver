package com.rich.file.mq.producer;

import cn.hutool.core.lang.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传任务生产者
 * 负责发送文件上传消息到RabbitMQ队列
 *
 * @author DuRuiChi
 * @create 2026-05-06
 */
@Component
@Slf4j
public class FileUploadProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Value("${file.upload.exchange:file.exchange}")
    private String fileExchange;

    @Value("${file.upload.routing-key:upload.file}")
    private String uploadFileRoutingKey;

    private static final String TASK_TYPE = "UPLOAD_FILE";

    /**
     * 发送文件上传任务
     *
     * @param localFilePath 本地文件路径
     * @param ossPath OSS存储路径
     * @param bucketName 存储桶名称
     * @param userId 用户ID
     * @return 消息ID
     */
    public String sendUploadTask(String localFilePath, String ossPath, String bucketName, Long userId) {
        try {
            String messageId = UUID.randomUUID().toString(true);

            // 构建消息体
            Map<String, Object> message = new HashMap<>();
            message.put("messageId", messageId);
            message.put("taskType", TASK_TYPE);
            message.put("userId", userId);
            message.put("payload", Map.of(
                    "localFilePath", localFilePath,
                    "ossPath", ossPath,
                    "bucketName", bucketName
            ));
            message.put("retryCount", 0);
            message.put("maxRetries", 3);
            message.put("createTime", LocalDateTime.now().toString());

            // 发送消息到RabbitMQ
            rabbitTemplate.convertAndSend(fileExchange, uploadFileRoutingKey, message);

            log.info("发送文件上传任务成功: messageId={}, localFilePath={}, ossPath={}", 
                    messageId, localFilePath, ossPath);
            return messageId;

        } catch (Exception e) {
            log.error("发送文件上传任务失败: localFilePath={}, error={}", localFilePath, e.getMessage(), e);
            return null;
        }
    }
}
