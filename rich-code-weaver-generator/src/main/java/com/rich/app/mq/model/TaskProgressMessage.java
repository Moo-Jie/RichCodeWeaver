package com.rich.app.mq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 任务进度消息
 * 用于WebSocket推送任务进度
 *
 * @author DuRuiChi
 * @create 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskProgressMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 产物ID
     */
    private Long appId;

    /**
     * 任务状态
     * PENDING: 待处理
     * RUNNING: 执行中
     * SUCCESS: 成功
     * FAILED: 失败
     * RETRYING: 重试中
     */
    private String status;

    /**
     * 任务进度(0-100)
     */
    private Integer progress;

    /**
     * 进度描述信息
     */
    private String message;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 任务结果(成功时为输出路径，失败时为错误信息)
     */
    private String result;
}
