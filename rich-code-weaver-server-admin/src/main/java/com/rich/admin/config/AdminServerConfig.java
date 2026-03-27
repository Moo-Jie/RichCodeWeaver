package com.rich.admin.config;

import com.rich.admin.notifier.CustomMailNotifier;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.notify.CompositeNotifier;
import de.codecentric.boot.admin.server.notify.LoggingNotifier;
import de.codecentric.boot.admin.server.notify.Notifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Spring Boot Admin Server 通知器配置类
 * 注册复合通知器，当被监控服务的状态发生变化时，
 * 同时通过日志记录和邮件发送两种方式进行通知。
 *
 * @author Rich
 */
@Configuration
public class AdminServerConfig {

    /**
     * 配置复合通知器，组合多个通知器
     * LoggingNotifier 将状态变化记录到日志
     * CustomMailNotifier 发送邮件通知给配置的收件人
     *
     * @param repository   实例仓库，用于查询服务实例信息
     * @param mailNotifier 自定义邮件通知器
     * @return 复合通知器
     */
    @Bean
    public Notifier notifier(InstanceRepository repository,
                             CustomMailNotifier mailNotifier) {
        LoggingNotifier loggingNotifier = new LoggingNotifier(repository);
        return new CompositeNotifier(Arrays.asList(loggingNotifier, mailNotifier));
    }
}
