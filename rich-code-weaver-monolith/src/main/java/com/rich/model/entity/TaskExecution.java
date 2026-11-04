package com.rich.model.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务执行记录实体
 *
 * @author DuRuiChi
 * @create 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("task_execution")
public class TaskExecution implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

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
     * 任务状态
     */
    private String status;

    /**
     * 任务进度(0-100)
     */
    private Integer progress;

    /**
     * 任务结果
     */
    private String result;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    private Integer maxRetries;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
