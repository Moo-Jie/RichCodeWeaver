package com.rich.file.mq.consumer;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.rabbitmq.client.Channel;
import com.rich.file.config.OSSConfig;
import com.rich.file.mq.config.RabbitMQConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * 文件上传任务消费者
 * 负责消费文件上传消息并执行上传任务
 *
 * @author DuRuiChi
 * @create 2026-05-06
 */
@Component
@Slf4j
public class FileUploadConsumer {

    @Resource
    private OSSConfig ossConfig;

    @Resource
    private RabbitTemplate rabbitTemplate;

    private static final String UPLOAD_FILE_QUEUE = RabbitMQConfig.UPLOAD_FILE_QUEUE;
    private static final long RETRY_INITIAL_INTERVAL_MS = 3000L;

    /**
     * 消费文件上传任务
     */
    @RabbitListener(queues = UPLOAD_FILE_QUEUE)
    public void consumeUploadTask(Map<String, Object> message, Channel channel, Message amqpMessage,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        String messageId = (String) message.get("messageId");
        
        try {
            log.info("开始处理文件上传任务: messageId={}", messageId);

            // 1. 获取任务参数
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) message.get("payload");
            String localFilePath = (String) payload.get("localFilePath");
            String ossPath = (String) payload.get("ossPath");
            String bucketName = (String) payload.get("bucketName");

            // 2. 校验本地文件
            File localFile = new File(localFilePath);
            if (!localFile.exists() || !localFile.isFile()) {
                throw new RuntimeException("本地文件不存在: " + localFilePath);
            }

            // 3. 上传到OSS
            log.info("开始上传文件到OSS: localFilePath={}, ossPath={}", localFilePath, ossPath);
            String fileUrl = uploadToOSS(localFile, ossPath, bucketName);

            if (StrUtil.isBlank(fileUrl)) {
                throw new RuntimeException("文件上传失败，返回URL为空");
            }

            log.info("文件上传成功: messageId={}, fileUrl={}", messageId, fileUrl);

            // 4. 删除本地临时文件
            boolean deleted = FileUtil.del(localFile);
            if (deleted) {
                log.info("删除本地临时文件成功: {}", localFilePath);
            } else {
                log.warn("删除本地临时文件失败: {}", localFilePath);
            }

            // 5. 手动ACK
            channel.basicAck(deliveryTag, false);

            log.info("文件上传任务完成: messageId={}, fileUrl={}", messageId, fileUrl);

        } catch (Exception e) {
            log.error("文件上传任务失败: messageId={}, error={}", messageId, e.getMessage(), e);
            
            // 失败处理
            handleUploadFailure(message, channel, deliveryTag, e);
        }
    }

    /**
     * 上传文件到OSS
     */
    private String uploadToOSS(File file, String ossPath, String bucketName) {
        OSS ossClient = null;
        try {
            // 创建OSS客户端
            ossClient = new OSSClientBuilder().build(
                    ossConfig.getEndPoint(),
                    ossConfig.getAccessKeyId(),
                    ossConfig.getAccessKeySecret()
            );

            // 上传文件
            try (FileInputStream inputStream = new FileInputStream(file)) {
                ossClient.putObject(bucketName, ossPath, inputStream);
            }

            // 构建文件访问URL
            String fileUrl = "https://" + bucketName + "." + ossConfig.getEndPoint() + "/" + ossPath;
            log.info("文件上传到OSS成功: ossPath={}, fileUrl={}", ossPath, fileUrl);
            
            return fileUrl;

        } catch (Exception e) {
            log.error("上传文件到OSS失败: ossPath={}, error={}", ossPath, e.getMessage(), e);
            throw new RuntimeException("上传文件到OSS失败: " + e.getMessage(), e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 处理上传失败
     */
    private void handleUploadFailure(Map<String, Object> message, Channel channel, 
                                      long deliveryTag, Exception e) {
        try {
            int retryCount = message.get("retryCount") instanceof Number 
                    ? ((Number) message.get("retryCount")).intValue() : 0;
            int maxRetries = message.get("maxRetries") instanceof Number 
                    ? ((Number) message.get("maxRetries")).intValue() : 3;

            if (retryCount < maxRetries) {
                // 指数退避: 3s, 6s, 12s
                long backoffMs = RETRY_INITIAL_INTERVAL_MS * (1L << retryCount);
                log.warn("文件上传失败,准备重试: messageId={}, retryCount={}/{}, backoff={}ms", 
                        message.get("messageId"), retryCount + 1, maxRetries, backoffMs);

                message.put("retryCount", retryCount + 1);

                // 指数退避等待
                Thread.sleep(backoffMs);

                // 重新发布带有新retryCount的消息
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.FILE_EXCHANGE,
                        RabbitMQConfig.UPLOAD_FILE_ROUTING_KEY,
                        message
                );

                // ACK原始消息
                channel.basicAck(deliveryTag, false);

            } else {
                // 超过最大重试次数,标记为失败
                log.error("文件上传失败且已达最大重试次数: messageId={}", message.get("messageId"));

                // 确认消费,不再重试
                channel.basicAck(deliveryTag, false);
            }
        } catch (Exception ex) {
            log.error("重试处理失败: messageId={}", message.get("messageId"), ex);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException nackEx) {
                log.error("NACK失败: messageId={}", message.get("messageId"), nackEx);
            }
        }
    }
}
