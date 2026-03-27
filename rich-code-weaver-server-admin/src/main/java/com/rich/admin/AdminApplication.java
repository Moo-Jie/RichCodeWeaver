package com.rich.admin;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot Admin Server 启动类
 * <p>
 * 提供服务监控面板，统一管理各微服务模块的健康状态、日志、端点等信息。
 * 当被监控的服务状态发生变化时，可通过邮件通知相关人员。
 * </p>
 *
 * @author Rich
 */
@EnableAdminServer
@SpringBootApplication
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
