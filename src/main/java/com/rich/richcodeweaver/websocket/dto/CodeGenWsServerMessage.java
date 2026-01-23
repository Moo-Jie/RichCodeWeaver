package com.rich.richcodeweaver.websocket.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 后端 -> 前端 WebSocket 消息
 */
@Data
@Builder
public class CodeGenWsServerMessage {
    /**
     * started | chunk | end | error | pong
     */
    private String type;

    private String taskId;
    /**
     * 注意：必须用字符串传输，避免前端 JS Number 精度丢失（雪花 ID 会被改写）
     */
    private String appId;

    /**
     * chunk 序号，从 1 开始
     */
    private Long seq;

    /**
     * 文本块（可直接追加到聊天窗口）
     */
    private String data;

    /**
     * error 消息
     */
    private String message;
}


