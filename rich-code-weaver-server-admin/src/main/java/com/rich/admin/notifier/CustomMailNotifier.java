package com.rich.admin.notifier;

import com.rich.admin.config.NotificationEmailProperties;
import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.domain.events.InstanceEvent;
import de.codecentric.boot.admin.server.domain.events.InstanceStatusChangedEvent;
import de.codecentric.boot.admin.server.notify.AbstractStatusChangeNotifier;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 自定义邮件通知器
 * 邮件内容为 HTML 格式，包含服务名称、状态变化、变化时间、服务地址及健康详情等信息。
 *
 * @author Rich
 */
@Slf4j
@Component
public class CustomMailNotifier extends AbstractStatusChangeNotifier {

    private final JavaMailSender mailSender;
    private final NotificationEmailProperties emailProperties;

    public CustomMailNotifier(InstanceRepository repository,
                              @Autowired(required = false) JavaMailSender mailSender,
                              NotificationEmailProperties emailProperties) {
        super(repository);
        this.mailSender = mailSender;
        this.emailProperties = emailProperties;
    }

    /**
     * 处理服务状态变化事件
     * 仅在邮件通知启用且事件类型为 InstanceStatusChangedEvent 时，
     * 根据场景配置判断是否发送通知邮件。
     *
     * @param event    实例事件
     * @param instance 服务实例
     * @return Mono&lt;Void&gt;
     */
    @Override
    protected Mono<Void> doNotify(InstanceEvent event, Instance instance) {
        return Mono.fromRunnable(() -> {
            if (mailSender == null) {
                log.warn("JavaMailSender 未配置，无法发送邮件通知");
                return;
            }

            if (!emailProperties.getEnabled()) {
                log.debug("邮件通知已禁用，跳过发送");
                return;
            }

            if (!(event instanceof InstanceStatusChangedEvent)) {
                return;
            }

            InstanceStatusChangedEvent statusEvent = (InstanceStatusChangedEvent) event;
            String newStatus = statusEvent.getStatusInfo().getStatus();

            if (!shouldNotify(newStatus)) {
                log.debug("根据配置，状态 {} 不发送通知", newStatus);
                return;
            }

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
     * 根据场景配置判断指定状态是否需要发送通知
     *
     * @param status 服务状态（UP / DOWN / OFFLINE / UNKNOWN）
     * @return true 表示需要发送通知
     */
    private boolean shouldNotify(String status) {
        NotificationEmailProperties.Scenarios scenarios = emailProperties.getScenarios();

        return switch (status.toUpperCase()) {
            case "UP" -> scenarios.getServiceUp();
            case "DOWN" -> scenarios.getServiceDown();
            case "OFFLINE" -> scenarios.getServiceOffline();
            case "UNKNOWN" -> scenarios.getServiceUnknown();
            default -> false;
        };
    }

    /**
     * 构建并发送 HTML 格式的通知邮件
     *
     * @param instance 服务实例
     * @param event    状态变化事件
     * @throws MessagingException 邮件发送异常
     */
    private void sendMail(Instance instance, InstanceStatusChangedEvent event) throws MessagingException {
        String serviceName = instance.getRegistration().getName();
        String oldStatus = event.getStatusInfo().getStatus();
        String newStatus = instance.getStatusInfo().getStatus();
        String serviceUrl = instance.getRegistration().getServiceUrl();

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        if (emailProperties.getRecipients().isEmpty()) {
            log.warn("未配置收件人邮箱，无法发送邮件");
            return;
        }
        helper.setTo(emailProperties.getRecipients().toArray(new String[0]));

        if (!emailProperties.getCc().isEmpty()) {
            helper.setCc(emailProperties.getCc().toArray(new String[0]));
        }

        // 从配置读取发件人，格式如: "RichCodeWeaver 监控系统 <xxx@qq.com>"
        helper.setFrom(emailProperties.getFrom());

        String subject = String.format("【RichCodeWeaver】服务状态变化: %s - %s", serviceName, newStatus);
        helper.setSubject(subject);

        String content = buildEmailContent(serviceName, oldStatus, newStatus, serviceUrl, instance);
        helper.setText(content, true);

        mailSender.send(message);
    }

    /**
     * 构建 HTML 格式的邮件正文
     *
     * @param serviceName 服务名称
     * @param oldStatus   旧状态
     * @param newStatus   新状态
     * @param serviceUrl  服务地址
     * @param instance    服务实例（用于获取健康详情）
     * @return HTML 邮件内容
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
        content.append(".status { font-size: 24px; font-weight: bold; color: ").append(statusColor).append("; }");
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

        // 健康详情
        if (instance.getStatusInfo().getDetails() != null && !instance.getStatusInfo().getDetails().isEmpty()) {
            content.append("<div class='info-row'>");
            content.append("<span class='label'>健康详情：</span>");
            content.append("<pre style='background-color: #f5f5f5; padding: 10px; border-radius: 3px; overflow-x: auto;'>");
            content.append(instance.getStatusInfo().getDetails().toString());
            content.append("</pre>");
            content.append("</div>");
        }

        // 管理面板链接
        content.append("<div class='info-row' style='text-align: center; margin-top: 20px;'>");
        content.append("<a href='http://localhost:9001' style='display: inline-block; padding: 10px 20px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 5px;'>");
        content.append("查看监控面板");
        content.append("</a>");
        content.append("</div>");

        content.append("</div>");

        // 页脚
        content.append("<div class='footer'>");
        content.append("<p>这是一封自动发送的邮件，请勿直接回复。</p>");
        content.append("<p>RichCodeWeaver AI 智能产物生成平台 - 服务监控系统</p>");
        content.append("</div>");

        content.append("</div>");
        content.append("</body>");
        content.append("</html>");

        return content.toString();
    }

    /**
     * 根据服务状态返回对应的显示颜色
     *
     * @param status 服务状态
     * @return CSS 颜色值
     */
    private String getStatusColor(String status) {
        return switch (status.toUpperCase()) {
            case "UP" -> "#4CAF50";       // 绿色 — 正常运行
            case "DOWN" -> "#F44336";     // 红色 — 服务宕机
            case "OFFLINE" -> "#FF9800";  // 橙色 — 服务离线
            case "UNKNOWN" -> "#9E9E9E";  // 灰色 — 状态未知
            default -> "#2196F3";         // 蓝色 — 其他状态
        };
    }

    /**
     * 根据服务状态返回对应的图标
     *
     * @param status 服务状态
     * @return 状态图标字符
     */
    private String getStatusIcon(String status) {
        return switch (status.toUpperCase()) {
            case "UP" -> "✅";
            case "DOWN" -> "❌";
            case "OFFLINE" -> "⚠️";
            case "UNKNOWN" -> "❓";
            default -> "ℹ️";
        };
    }
}
