package com.rich.richcodeweaverserveradmin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.ArrayList;
import java.util.List;

/**
 * 邮件通知配置属性
 * 从 notification-emails.yml 文件中读取配置
 * 
 * @author Rich
 * @date 2025-12-12
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "notification.emails")
@PropertySource(value = "classpath:notification-emails.yml", factory = YamlPropertySourceFactory.class)
public class NotificationEmailProperties {
    
    /**
     * 收件人邮箱列表
     */
    private List<String> recipients = new ArrayList<>();
    
    /**
     * 抄送邮箱列表
     */
    private List<String> cc = new ArrayList<>();
    
    /**
     * 是否启用邮件通知
     */
    private Boolean enabled = true;
    
    /**
     * 通知场景配置
     */
    private Scenarios scenarios = new Scenarios();
    
    /**
     * 通知场景配置类
     */
    @Data
    public static class Scenarios {
        /**
         * 服务上线通知
         */
        private Boolean serviceUp = true;
        
        /**
         * 服务下线通知
         */
        private Boolean serviceDown = true;
        
        /**
         * 服务状态未知通知
         */
        private Boolean serviceUnknown = false;
        
        /**
         * 服务离线通知
         */
        private Boolean serviceOffline = true;
    }
}

