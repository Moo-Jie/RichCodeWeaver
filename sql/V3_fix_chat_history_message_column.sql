-- V3: 修复 chat_history 表 message 列长度不足问题
-- 原因：AI 响应内容（含代码块）可能超过 TEXT 类型的 65535 字节限制
-- 解决：将 message 列从 TEXT 改为 MEDIUMTEXT（最大 16MB）

ALTER TABLE `chat_history` MODIFY COLUMN `message` MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息';
