-- ============================================================
-- AI 对话增量摘要表
-- 用于存储每个会话的近期对话摘要，供 LLM 上下文补充使用
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_chat_summary` (
    `id`                    BIGINT          NOT NULL COMMENT '主键（雪花ID）',
    `memoryId`              BIGINT          NOT NULL COMMENT '记忆标识（appId 或 conversationId）',
    `summaryType`           VARCHAR(32)     NOT NULL COMMENT '摘要类型：AGENT / WORKFLOW / CUSTOMER_SERVICE',
    `summaryContent`        TEXT            NOT NULL COMMENT '摘要内容（结构化文本）',
    `summarizedRoundCount`  INT             NOT NULL DEFAULT 0 COMMENT '已摘要的累计对话轮数',
    `createTime`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`              TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_memory_type` (`memoryId`, `summaryType`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 对话增量摘要表';

-- ============================================================
-- 插入摘要生成系统提示词（落库，运行时由 Dubbo 查询 MySQL 获取）
-- ============================================================
INSERT INTO `system_prompt` (`id`, `promptName`, `promptKey`, `promptContent`, `description`, `userId`, `createTime`, `updateTime`, `isDelete`)
VALUES (
    1924000000000000001,
    'AI对话增量摘要生成提示词',
    'ai-chat-summary-system-prompt',
    '请浓缩整合历史对话摘要与新增对话内容，生成精简统一的近期对话摘要，严格遵守以下规则：\n1. 强制保留：用户核心需求、双方关键结论、业务约定信息、未完成待处理事项\n2. 强制剔除：口语语气词、重复表述、无关闲聊、无效对话内容\n3. 严格精简：控制整体摘要篇幅在100字以内，只留存高价值关键信息\n4. 工具调用类对话优先保留调用目的，精简执行过程冗余内容\n\n固定输出结构化格式，禁止自由发挥：\n核心背景：\n用户关键诉求：\n已达成结论：\n待后续处理：',
    '用于 AI 对话增量摘要生成，每累计5轮对话触发一次LLM浓缩',
    0,
    NOW(),
    NOW(),
    0
);
