package com.rich.file.mq.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类 - File模块
 * 保留文件上传队列常量与监听器容器参数
 *
 * @author DuRuiChi
 * @create 2026-05-06
 */
@Configuration("fileRabbitMQConfig")
public class RabbitMQConfig {

    // 队列名称常量
    public static final String UPLOAD_FILE_QUEUE = "upload.file.queue";
    public static final String DLX_QUEUE = "dlx.queue";

    // 交换机名称常量
    public static final String FILE_EXCHANGE = "file.exchange";
    public static final String DLX_EXCHANGE = "dlx.exchange";

    // 路由键常量
    public static final String UPLOAD_FILE_ROUTING_KEY = "upload.file";
    public static final String DLX_ROUTING_KEY = "dlx";

    /**
     * 文件上传监听器容器工厂
     * 保持独立并发参数，避免影响其他 RabbitMQ 消费者
     */
    @Bean("fileRabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory fileRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(5);
        factory.setPrefetchCount(1);
        return factory;
    }
}
