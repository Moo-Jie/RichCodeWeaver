package com.rich.file;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * 文件服务模块启动类
 * 负责启动文件上传服务，提供OSS文件上传和截图功能
 *
 * @author DuRuiChi
 * @since 2026-03-11
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@ComponentScan("com.rich")
@EnableDubbo
public class FileApplication {

    /**
     * 应用启动入口方法
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(FileApplication.class, args);
    }
}
