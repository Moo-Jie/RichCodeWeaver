package com.rich.file.mq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类 - File模块
 * 配置文件上传队列相关配置
 *
 * @author DuRuiChi
 * @create 2026-05-06
 */
@Configuration
@Slf4j
public class RabbitMQConfig {

    // ==================== 队列名称常量 ====================
    
    public static final String UPLOAD_FILE_QUEUE = "upload.file.queue";
    public static final String DLX_QUEUE = "dlx.queue";
    
    // ==================== 交换机名称常量 ====================
    
    public static final String FILE_EXCHANGE = "file.exchange";
    public static final String DLX_EXCHANGE = "dlx.exchange";
    
    // ==================== 路由键常量 ====================
    
    public static final String UPLOAD_FILE_ROUTING_KEY = "upload.file";
    public static final String DLX_ROUTING_KEY = "dlx";
    
    // ==================== 消息转换器 ====================
    
    /**
     * 配置JSON消息转换器
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    // ==================== RabbitTemplate配置 ====================
    
    /**
     * 配置RabbitTemplate
     * 启用消息确认机制和返回机制
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        
        // 启用消息发送确认
        template.setMandatory(true);
        
        // 消息发送到交换机确认回调
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.debug("消息发送到交换机成功: correlationData={}", correlationData);
            } else {
                log.error("消息发送到交换机失败: correlationData={}, cause={}", correlationData, cause);
            }
        });
        
        // 消息从交换机路由到队列失败回调
        template.setReturnsCallback(returned -> {
            log.error("消息路由到队列失败: message={}, replyCode={}, replyText={}, exchange={}, routingKey={}",
                    returned.getMessage(), returned.getReplyCode(), returned.getReplyText(),
                    returned.getExchange(), returned.getRoutingKey());
        });
        
        return template;
    }
    
    // ==================== 监听器容器工厂配置 ====================
    
    /**
     * 配置监听器容器工厂
     * 设置手动ACK模式和并发消费者数量
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        
        // 设置手动ACK模式
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        
        // 设置并发消费者数量
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(5);
        
        // 设置预取数量(每个消费者一次最多拉取的消息数)
        factory.setPrefetchCount(1);
        
        return factory;
    }
    
    // ==================== 死信交换机和队列 ====================
    
    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }
    
    /**
     * 死信队列
     */
    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable(DLX_QUEUE).build();
    }
    
    /**
     * 死信队列绑定
     */
    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue())
                .to(dlxExchange())
                .with(DLX_ROUTING_KEY);
    }
    
    // ==================== 文件上传队列配置 ====================
    
    /**
     * 文件上传队列
     * 配置消息TTL为5分钟
     */
    @Bean
    public Queue uploadFileQueue() {
        return QueueBuilder.durable(UPLOAD_FILE_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5分钟TTL
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLX_ROUTING_KEY)
                .build();
    }
    
    /**
     * 文件交换机
     */
    @Bean
    public DirectExchange fileExchange() {
        return new DirectExchange(FILE_EXCHANGE, true, false);
    }
    
    /**
     * 文件上传队列绑定
     */
    @Bean
    public Binding uploadFileBinding() {
        return BindingBuilder.bind(uploadFileQueue())
                .to(fileExchange())
                .with(UPLOAD_FILE_ROUTING_KEY);
    }
}
