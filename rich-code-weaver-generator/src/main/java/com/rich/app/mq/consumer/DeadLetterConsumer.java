package com.rich.app.mq.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rich.app.mq.config.RabbitMQConfig;
import com.rich.app.service.TaskExecutionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 死信队列消费者
 * 统一处理所有失败的消息
 *
 * @author DuRuiChi
 * @create 2026-05-06
 */
@Component
@Slf4j
public class DeadLetterConsumer {

    @Resource
    private TaskExecutionService taskExecutionService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 消费死信消息
     */
    @RabbitListener(queues = RabbitMQConfig.DLX_QUEUE)
    public void consumeDeadLetter(Message message, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            // 获取消息内容
            String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
            
            // 获取原始队列信息
            String originalExchange = message.getMessageProperties().getReceivedExchange();
            String originalRoutingKey = message.getMessageProperties().getReceivedRoutingKey();
            
            // 解析消息体提取任务信息
            Long appId = null;
            String taskType = null;
            try {
                JsonNode node = objectMapper.readTree(messageBody);
                if (node.has("appId")) appId = node.get("appId").asLong();
                if (node.has("taskType")) taskType = node.get("taskType").asText();
            } catch (Exception ignored) {
                // 解析失败不影响死信处理流程
            }

            // 结构化告警日志（便于监控平台检索）
            log.error("[DEAD_LETTER_ALERT] exchange={}, routingKey={}, taskType={}, appId={}, message={}", 
                    originalExchange, originalRoutingKey, taskType, appId, messageBody);

            // 尝试更新任务状态为失败
            if (appId != null) {
                try {
                    taskExecutionService.updateStatus(appId, "FAILED", 0);
                    taskExecutionService.updateResult(appId, "任务超时进入死信队列");
                } catch (Exception e) {
                    log.warn("更新死信任务状态失败: appId={}", appId, e);
                }
            }

            // 确认消费死信消息
            channel.basicAck(deliveryTag, false);

        } catch (IOException e) {
            log.error("处理死信消息失败", e);
            try {
                // 拒绝消息，不重新入队
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ex) {
                log.error("拒绝死信消息失败", ex);
            }
        }
    }
}
