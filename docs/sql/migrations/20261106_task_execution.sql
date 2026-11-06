-- 任务执行记录表补齐脚本
-- 说明：本脚本未在审查过程中执行；当前 TaskExecutionService 已提供 Redis 降级。

CREATE TABLE IF NOT EXISTS `task_execution` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_type` varchar(50) NOT NULL COMMENT '任务类型(BUILD_VUE_PROJECT/GENERATE_SCREENSHOT/UPLOAD_FILE)',
  `app_id` bigint NOT NULL COMMENT '产物ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID（截图等系统任务可为空）',
  `status` varchar(20) NOT NULL COMMENT '任务状态(PENDING/RUNNING/SUCCESS/FAILED/RETRYING)',
  `progress` int DEFAULT '0' COMMENT '任务进度(0-100)',
  `result` text COMMENT '任务结果(成功时为输出路径,失败时为错误信息)',
  `retry_count` int DEFAULT '0' COMMENT '重试次数',
  `max_retries` int DEFAULT '3' COMMENT '最大重试次数',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_app_id` (`app_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务执行记录表';
