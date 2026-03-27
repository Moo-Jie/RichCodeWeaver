package com.rich.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 邮件通知配置属性
 * 包括发件人、收件人列表、抄送列表、通知开关及通知场景等。
 *
 * @author Rich
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "notification.emails")
public class NotificationEmailProperties {

    /**
     * 发件人显示名称和邮箱，格式如: "RichCodeWeaver 监控系统 <xxx@qq.com>"
     */
    private String from;

    /**
     * 收件人邮箱列表
     */
    private List<String> recipients = new ArrayList<>();

    /**
     * 抄送邮箱列表
     */
    private List<String> cc = new ArrayList<>();

    /**
     * 是否启用邮件通知（默认启用）
     */
    private Boolean enabled = true;

    /**
     * 通知场景配置
     */
    private Scenarios scenarios = new Scenarios();

    /**
     * 通知场景开关配置
     * 可针对不同的服务状态变化事件，独立控制是否发送邮件通知。
     */
    @Data
    public static class Scenarios {

        /**
         * 服务上线（UP）时是否发送通知
         */
        private Boolean serviceUp = true;

        /**
         * 服务下线（DOWN）时是否发送通知
         */
        private Boolean serviceDown = true;

        /**
         * 服务状态未知（UNKNOWN）时是否发送通知
         */
        private Boolean serviceUnknown = false;

        /**
         * 服务离线（OFFLINE）时是否发送通知
         */
        private Boolean serviceOffline = true;
    }
}
