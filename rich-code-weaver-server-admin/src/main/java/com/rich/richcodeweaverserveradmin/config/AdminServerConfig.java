package com.rich.richcodeweaverserveradmin.config;

import com.rich.richcodeweaverserveradmin.notifier.CustomMailNotifier;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.notify.CompositeNotifier;
import de.codecentric.boot.admin.server.notify.LoggingNotifier;
import de.codecentric.boot.admin.server.notify.Notifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Spring Boot Admin Server 配置类
 * 
 * @author Rich
 * @date 2025-12-12
 */
@Configuration
public class AdminServerConfig {

    /**
     * 配置通知器
     * 当服务状态发生变化时，发送通知
     * 
     * @param repository 实例仓库
     * @param mailNotifier 邮件通知器
     * @return 复合通知器
     */
    @Bean
    public Notifier notifier(InstanceRepository repository, 
                            CustomMailNotifier mailNotifier) {
        // 使用日志通知器，将状态变化记录到日志
        LoggingNotifier loggingNotifier = new LoggingNotifier(repository);
        
        // 组合多个通知器：日志 + 邮件
        // 当服务状态变化时，会同时：
        // 1. 记录日志（LoggingNotifier）
        // 2. 发送邮件（CustomMailNotifier）
        return new CompositeNotifier(Arrays.asList(loggingNotifier, mailNotifier));
    }
}

