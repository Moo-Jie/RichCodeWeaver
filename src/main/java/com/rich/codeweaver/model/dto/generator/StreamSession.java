package com.rich.codeweaver.model.dto.generator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * SSE 流式会话实体，用于支持断线重连
 *
 * @author DuRuiChi
 * @create 2025/12/27
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamSession {
    /**
     * 会话ID（由 appId + userId + timestamp 组成）
     */
    private String sessionId;

    /**
     * 产物ID
     */
    private Long appId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话创建时间
     */
    private LocalDateTime createTime;

    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessTime;

    /**
     * 流式事件缓存队列（存储已生成的事件）
     */
    private ConcurrentLinkedQueue<StreamEvent> eventQueue;

    /**
     * 流是否已完成
     */
    private volatile boolean completed;

    /**
     * 流是否发生错误
     */
    private volatile boolean hasError;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 当前事件ID计数器
     */
    private volatile long eventIdCounter;
}
