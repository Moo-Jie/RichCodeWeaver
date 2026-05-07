-- 任务执行记录表
CREATE TABLE IF NOT EXISTS task_execution (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_type VARCHAR(50) NOT NULL COMMENT '任务类型(BUILD_VUE_PROJECT/GENERATE_SCREENSHOT/UPLOAD_FILE)',
    app_id BIGINT NOT NULL COMMENT '产物ID',
    user_id BIGINT DEFAULT NULL COMMENT '用户ID（截图等系统任务可为空）',
    status VARCHAR(20) NOT NULL COMMENT '任务状态(PENDING/RUNNING/SUCCESS/FAILED/RETRYING)',
    progress INT DEFAULT 0 COMMENT '任务进度(0-100)',
    result TEXT COMMENT '任务结果(成功时为输出路径,失败时为错误信息)',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    max_retries INT DEFAULT 3 COMMENT '最大重试次数',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_app_id (app_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务执行记录表';
