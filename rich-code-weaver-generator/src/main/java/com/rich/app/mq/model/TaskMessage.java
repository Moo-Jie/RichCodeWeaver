package com.rich.app.mq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 任务消息基类
 * 所有任务消息的通用结构
 *
 * @author DuRuiChi
 * @create 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息唯一ID
     */
    private String messageId;

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 产物ID
     */
    private Long appId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 任务负载数据(JSON格式)
     */
    private Map<String, Object> payload;

    /**
     * 当前重试次数
     */
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    private Integer maxRetries;

    /**
     * 消息创建时间
     */
    private LocalDateTime createTime;

    /**
     * 链路追踪ID
     */
    private String traceId;
}
