-- ============================================================
-- V5: 用户聊天与好友模块
-- 包含: user_friendship(好友关系), chat_conversation(会话), chat_message(消息)
-- ============================================================

-- -----------------------------------------------------------
-- 1. 好友关系表
-- 记录用户之间的好友申请与关系状态
-- status: 0=待处理, 1=已同意, 2=已拒绝
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_friendship` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `userId`     BIGINT       NOT NULL COMMENT '发起方用户id',
    `friendId`   BIGINT       NOT NULL COMMENT '接收方用户id',
    `status`     TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0=待处理, 1=已同意, 2=已拒绝',
    `remark`     VARCHAR(200) DEFAULT NULL COMMENT '申请备注',
    `createTime` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_friend` (`userId`, `friendId`),
    INDEX `idx_friendId` (`friendId`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系表';

-- -----------------------------------------------------------
-- 2. 会话表
-- 记录两个用户之间的对话会话元数据
-- 每对用户只有一条会话记录，userIdSmall < userIdLarge 保证唯一性
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `chat_conversation` (
    `id`              BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `userIdSmall`     BIGINT   NOT NULL COMMENT '用户id(较小值)',
    `userIdLarge`     BIGINT   NOT NULL COMMENT '用户id(较大值)',
    `lastMessageId`   BIGINT   DEFAULT NULL COMMENT '最后一条消息id',
    `lastMessageTime` DATETIME DEFAULT NULL COMMENT '最后一条消息时间',
    `createTime`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_pair` (`userIdSmall`, `userIdLarge`),
    INDEX `idx_lastMessageTime` (`lastMessageTime` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话表';

-- -----------------------------------------------------------
-- 3. 消息表
-- 存储用户之间的聊天消息内容
-- messageType: text=文本, image=图片（预留扩展）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `conversationId`  BIGINT       NOT NULL COMMENT '会话id',
    `senderId`        BIGINT       NOT NULL COMMENT '发送方用户id',
    `receiverId`      BIGINT       NOT NULL COMMENT '接收方用户id',
    `content`         TEXT         NOT NULL COMMENT '消息内容',
    `messageType`     VARCHAR(20)  NOT NULL DEFAULT 'text' COMMENT '消息类型: text=文本, image=图片',
    `isRead`          TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读: 0=未读, 1=已读',
    `createTime`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_conversationId` (`conversationId`),
    INDEX `idx_senderId` (`senderId`),
    INDEX `idx_receiverId` (`receiverId`),
    INDEX `idx_receiverId_isRead` (`receiverId`, `isRead`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';
