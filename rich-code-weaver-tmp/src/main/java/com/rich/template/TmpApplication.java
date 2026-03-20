package com.rich.template;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@MapperScan("com.rich.template.mapper")
@ComponentScan(value = "com.rich", excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {TmpApplication.class})
})
@EnableDubbo
public class TmpApplication {
    public static void main(String[] args) {
        SpringApplication.run(TmpApplication.class, args);
    }
}
