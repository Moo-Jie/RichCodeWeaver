package com.rich.richcodeweaverserveradmin.notifier;

import com.rich.richcodeweaverserveradmin.config.NotificationEmailProperties;
import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.domain.events.InstanceEvent;
import de.codecentric.boot.admin.server.domain.events.InstanceStatusChangedEvent;
import de.codecentric.boot.admin.server.notify.AbstractStatusChangeNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 自定义邮件通知器
 * 监控服务状态变化并发送邮件通知
 * 
 * @author Rich
 * @date 2025-12-12
 */
@Slf4j
@Component
public class CustomMailNotifier extends AbstractStatusChangeNotifier {
    
    private final JavaMailSender mailSender;
    private final NotificationEmailProperties emailProperties;
    private final String fromEmail;
    
    public CustomMailNotifier(InstanceRepository repository,
                             JavaMailSender mailSender,
                             NotificationEmailProperties emailProperties) {
        super(repository);
        this.mailSender = mailSender;
        this.emailProperties = emailProperties;
        // 从配置中提取发件人邮箱
        this.fromEmail = extractEmailFromConfig();
    }
    
    /**
     * 从配置字符串中提取邮箱地址
     * 例如: "RichCodeWeaver 监控系统 <1745966516@qq.com>" -> "1745966516@qq.com"
     */
    private String extractEmailFromConfig() {
        // 这里使用简单的方式，实际应该从 application.properties 中读取
        return "1745966516@qq.com"; // 需要与 application.properties 中的配置一致
    }
    
    @Override
    protected Mono<Void> doNotify(InstanceEvent event, Instance instance) {
        return Mono.fromRunnable(() -> {
            // 检查是否启用邮件通知
            if (!emailProperties.getEnabled()) {
                log.debug("邮件通知已禁用，跳过发送");
                return;
            }
            
            // 检查是否是状态变化事件
            if (!(event instanceof InstanceStatusChangedEvent)) {
                return;
            }
            
            InstanceStatusChangedEvent statusEvent = (InstanceStatusChangedEvent) event;
            String newStatus = statusEvent.getStatusInfo().getStatus();
            
            // 根据配置决定是否发送通知
            if (!shouldNotify(newStatus)) {
                log.debug("根据配置，状态 {} 不发送通知", newStatus);
                return;
            }
            
            // 发送邮件
            try {
                sendMail(instance, statusEvent);
                log.info("成功发送服务状态变化通知邮件: {} - {}", 
                    instance.getRegistration().getName(), newStatus);
            } catch (Exception e) {
                log.error("发送邮件通知失败", e);
            }
        });
    }
    
    /**
     * 根据配置判断是否应该发送通知
     */
    private boolean shouldNotify(String status) {
        NotificationEmailProperties.Scenarios scenarios = emailProperties.getScenarios();
        
        switch (status.toUpperCase()) {
            case "UP":
                return scenarios.getServiceUp();
            case "DOWN":
                return scenarios.getServiceDown();
            case "OFFLINE":
                return scenarios.getServiceOffline();
            case "UNKNOWN":
                return scenarios.getServiceUnknown();
            default:
                return false;
        }
    }
    
    /**
     * 发送邮件
     */
    private void sendMail(Instance instance, InstanceStatusChangedEvent event) throws MessagingException {
        String serviceName = instance.getRegistration().getName();
        String oldStatus = event.getStatusInfo().getStatus();
        String newStatus = instance.getStatusInfo().getStatus();
        String serviceUrl = instance.getRegistration().getServiceUrl();
        
        // 创建 HTML 邮件
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        // 设置收件人
        if (emailProperties.getRecipients().isEmpty()) {
            log.warn("未配置收件人邮箱，无法发送邮件");
            return;
        }
        helper.setTo(emailProperties.getRecipients().toArray(new String[0]));
        
        // 设置抄送
        if (!emailProperties.getCc().isEmpty()) {
            helper.setCc(emailProperties.getCc().toArray(new String[0]));
        }
        
        // 设置发件人
        helper.setFrom(fromEmail);
        
        // 设置主题
        String subject = String.format("【RichCodeWeaver】服务状态变化: %s - %s", serviceName, newStatus);
        helper.setSubject(subject);
        
        // 设置邮件内容（HTML格式）
        String content = buildEmailContent(serviceName, oldStatus, newStatus, serviceUrl, instance);
        helper.setText(content, true);
        
        // 发送邮件
        mailSender.send(message);
    }
    
    /**
     * 构建邮件内容（HTML格式）
     */
    private String buildEmailContent(String serviceName, String oldStatus, 
                                     String newStatus, String serviceUrl, 
                                     Instance instance) {
        String statusColor = getStatusColor(newStatus);
        String statusIcon = getStatusIcon(newStatus);
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        StringBuilder content = new StringBuilder();
        content.append("<!DOCTYPE html>");
        content.append("<html>");
        content.append("<head>");
        content.append("<meta charset='UTF-8'>");
        content.append("<style>");
        content.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
        content.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        content.append(".header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }");
        content.append(".content { background-color: #f9f9f9; padding: 20px; border: 1px solid #ddd; }");
        content.append(".status { font-size: 24px; font-weight: bold; color: " + statusColor + "; }");
        content.append(".info-row { margin: 10px 0; padding: 10px; background-color: white; border-left: 3px solid #4CAF50; }");
        content.append(".label { font-weight: bold; color: #666; }");
        content.append(".footer { margin-top: 20px; padding: 10px; text-align: center; color: #888; font-size: 12px; }");
        content.append("</style>");
        content.append("</head>");
        content.append("<body>");
        content.append("<div class='container'>");
        
        // 标题
        content.append("<div class='header'>");
        content.append("<h2>").append(statusIcon).append(" RichCodeWeaver 服务监控通知</h2>");
        content.append("</div>");
        
        // 内容
        content.append("<div class='content'>");
        
        // 状态变化
        content.append("<div class='info-row'>");
        content.append("<span class='label'>服务名称：</span>");
        content.append("<span style='font-size: 18px; font-weight: bold;'>").append(serviceName).append("</span>");
        content.append("</div>");
        
        content.append("<div class='info-row'>");
        content.append("<span class='label'>状态变化：</span>");
        content.append("<span>").append(oldStatus).append("</span>");
        content.append(" → ");
        content.append("<span class='status'>").append(newStatus).append("</span>");
        content.append("</div>");
        
        content.append("<div class='info-row'>");
        content.append("<span class='label'>变化时间：</span>");
        content.append("<span>").append(currentTime).append("</span>");
        content.append("</div>");
        
        content.append("<div class='info-row'>");
        content.append("<span class='label'>服务地址：</span>");
        content.append("<span><a href='").append(serviceUrl).append("'>").append(serviceUrl).append("</a></span>");
        content.append("</div>");
        
        // 健康信息
        if (instance.getStatusInfo().getDetails() != null && !instance.getStatusInfo().getDetails().isEmpty()) {
            content.append("<div class='info-row'>");
            content.append("<span class='label'>健康详情：</span>");
            content.append("<pre style='background-color: #f5f5f5; padding: 10px; border-radius: 3px; overflow-x: auto;'>");
            content.append(instance.getStatusInfo().getDetails().toString());
            content.append("</pre>");
            content.append("</div>");
        }
        
        // 管理链接
        content.append("<div class='info-row' style='text-align: center; margin-top: 20px;'>");
        content.append("<a href='http://localhost:9001' style='display: inline-block; padding: 10px 20px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 5px;'>");
        content.append("查看监控面板");
        content.append("</a>");
        content.append("</div>");
        
        content.append("</div>");
        
        // 页脚
        content.append("<div class='footer'>");
        content.append("<p>这是一封自动发送的邮件，请勿直接回复。</p>");
        content.append("<p>RichCodeWeaver AI智能应用生成平台 - 服务监控系统</p>");
        content.append("</div>");
        
        content.append("</div>");
        content.append("</body>");
        content.append("</html>");
        
        return content.toString();
    }
    
    /**
     * 获取状态对应的颜色
     */
    private String getStatusColor(String status) {
        switch (status.toUpperCase()) {
            case "UP":
                return "#4CAF50"; // 绿色
            case "DOWN":
                return "#F44336"; // 红色
            case "OFFLINE":
                return "#FF9800"; // 橙色
            case "UNKNOWN":
                return "#9E9E9E"; // 灰色
            default:
                return "#2196F3"; // 蓝色
        }
    }
    
    /**
     * 获取状态对应的图标
     */
    private String getStatusIcon(String status) {
        switch (status.toUpperCase()) {
            case "UP":
                return "✅"; // 启动
            case "DOWN":
                return "❌"; // 停止
            case "OFFLINE":
                return "⚠️"; // 离线
            case "UNKNOWN":
                return "❓"; // 未知
            default:
                return "ℹ️"; // 信息
        }
    }
}

