-- 系统提示词管理表（存储文件元数据，不存储文件内容）
CREATE TABLE IF NOT EXISTS `system_prompt`
(
    `id`          BIGINT       NOT NULL COMMENT 'id（雪花）',
    `promptName`  VARCHAR(128) NOT NULL COMMENT '提示词名称',
    `filePath`    VARCHAR(256) NOT NULL COMMENT '提示词文件路径（文件名或相对路径，如 html-system-prompt.txt）',
    `description` VARCHAR(512) NULL     DEFAULT NULL COMMENT '提示词描述',
    `userId`      BIGINT       NOT NULL COMMENT '创建人id',
    `createTime`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除（逻辑删除）',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='系统提示词';

-- 初始化已有的提示词文件数据（只存储文件名，绝对路径由配置 prompt.base-path 决定）
INSERT INTO `system_prompt` (`id`, `promptName`, `filePath`, `description`, `userId`)
VALUES (1, 'HTML代码生成', 'html-system-prompt.txt', 'AI 流式生成 HTML 代码的系统提示词', 0),
       (2, '多文件代码生成', 'multi-file-system-prompt.txt', 'AI 流式生成多文件代码的系统提示词', 0),
       (3, 'Vue项目代码生成', 'vue-project-system-prompt.txt', 'AI 流式推理生成 Vue 项目工程代码的系统提示词', 0),
       (4, '代码生成策略', 'code-generation-strategy-system-prompt.txt', 'AI 代码生成策略选择的系统提示词', 0),
       (5, '代码审查', 'code-review-system-prompt.txt', 'AI 代码审查的系统提示词', 0),
       (6, '图片资源处理', 'image-resource-system-prompt.txt', 'AI 图片资源处理的系统提示词', 0),
       (7, '提示词优化', 'prompt-optimization-system-prompt.txt', 'AI 提示词优化的系统提示词', 0),
       (8, '网页资源整理', 'web-resource-organize-system-prompt.txt', 'AI 网页资源整理的系统提示词', 0);
