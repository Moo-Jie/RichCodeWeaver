package com.rich.codeweaver.model.dto.generator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE 流式事件实体
 *
 * @author DuRuiChi
 * @create 2025/12/27
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamEvent {
    /**
     * 事件ID（用于断线重连时定位）
     */
    private String eventId;

    /**
     * 事件类型（data/error/end）
     */
    private String eventType;

    /**
     * 事件数据
     */
    private String data;

    /**
     * 事件创建时间戳
     */
    private long timestamp;
}
