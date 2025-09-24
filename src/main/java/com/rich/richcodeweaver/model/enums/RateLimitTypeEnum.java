package com.rich.richcodeweaver.model.enums;

/**
 * 限流类型枚举
 *
 * @author DuRuiChi
 * @create 2025/9/8
 **/
public enum RateLimitTypeEnum {

    /**
     * 针对具体接口限流
     */
    API,

    /**
     * 针对请求 IP 限流
     */
    IP,

    /**
     * 针对用户限流
     */
    USER,
}
