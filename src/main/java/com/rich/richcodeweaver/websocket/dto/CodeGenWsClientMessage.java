package com.rich.richcodeweaver.websocket.dto;

import lombok.Data;

/**
 * 前端 -> 后端 WebSocket 消息
 */
@Data
public class CodeGenWsClientMessage {
    /**
     * start | resume | cancel | ping
     */
    private String type;

    /**
     * 注意：必须用字符串传输，避免前端 JS Number 精度丢失（雪花 ID 会被改写）
     */
    private String appId;
    private String message;
    private Boolean isAgent;

    /**
     * 任务 id（resume/cancel）
     */
    private String taskId;

    /**
     * 断点续传：从哪个 seq 开始要数据（包含该 seq）
     * seq 从 1 开始
     */
    private Long fromSeq;
}


