package com.rich.model.annotation;

import com.rich.model.enums.RateLimitTypeEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限制接口的请求速率的注解
 *
 * @author DuRuiChi
 * @create 2025/8/5
 **/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /**
     * 窗口时间内的最大请求速率
     */
    int rate() default 10;

    /**
     * 窗口时间，单位：秒
     */
    int window() default 1;

    /**
     * 限流类型 (接口级别、用户级别、IP 级别)
     */
    RateLimitTypeEnum type() default RateLimitTypeEnum.API;
}
