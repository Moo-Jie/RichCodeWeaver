-- ===================================================
-- V7: 企业协作模块 - 产物协作者管理
-- ===================================================

-- 产物协作者表：记录产物的协作者关系
CREATE TABLE IF NOT EXISTS `app_collaborator` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `appId`       BIGINT       NOT NULL                COMMENT '产物id',
    `userId`      BIGINT       NOT NULL                COMMENT '协作者用户id',
    `inviterId`   BIGINT       NOT NULL                COMMENT '邀请人用户id（产物所有者）',
    `status`      TINYINT      NOT NULL DEFAULT 0      COMMENT '状态: 0=待确认, 1=已接受, 2=已拒绝, 3=已移除',
    `role`        VARCHAR(32)  NOT NULL DEFAULT 'editor' COMMENT '协作角色: editor(编辑者), viewer(查看者)',
    `createTime`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_user` (`appId`, `userId`),
    INDEX `idx_userId` (`userId`),
    INDEX `idx_appId_status` (`appId`, `status`),
    INDEX `idx_userId_status` (`userId`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产物协作者表';
