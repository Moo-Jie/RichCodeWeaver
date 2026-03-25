package com.rich.social;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * 社区模块启动类
 * 负责启动社区服务，提供热点数据、点赞、收藏、转发、评论等社交功能
 *
 * @author DuRuiChi
 * @since 2026-03-25
 */
@SpringBootApplication
@MapperScan("com.rich.social.mapper")
@ComponentScan(value = "com.rich", excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SocialApplication.class})
})
@EnableDubbo
public class SocialApplication {

    /**
     * 应用启动入口方法
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SocialApplication.class, args);
    }
}
