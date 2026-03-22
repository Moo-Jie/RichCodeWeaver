package com.rich.template;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * 提示词模板模块启动类
 * 负责启动提示词模板服务，提供模板管理和匹配功能
 *
 * @author DuRuiChi
 * @since 2026-03-12
 */
@SpringBootApplication
@MapperScan("com.rich.template.mapper")
@ComponentScan(value = "com.rich", excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {TmpApplication.class})
})
@EnableDubbo
public class TmpApplication {
    
    /**
     * 应用启动入口方法
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(TmpApplication.class, args);
    }
}
