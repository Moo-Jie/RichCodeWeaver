-- 包含：素材分类表、素材表
-- =====================================================

-- -----------------------------------------------------
-- 素材分类表
-- -----------------------------------------------------
DROP TABLE IF EXISTS `material_category`;
CREATE TABLE `material_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `categoryName` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `categoryCode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类编码（唯一标识）',
  `categoryIcon` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类图标（Ant Design图标名）',
  `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类描述',
  `sortOrder` int NOT NULL DEFAULT '0' COMMENT '排序权重（越小越靠前）',
  `isEnabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用',
  `userId` bigint DEFAULT NULL COMMENT '创建人id（NULL表示系统预置）',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_categoryCode` (`categoryCode`),
  KEY `idx_sortOrder` (`sortOrder`),
  KEY `idx_isEnabled` (`isEnabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='素材分类表';

-- 插入预置分类
INSERT INTO `material_category` (`categoryName`, `categoryCode`, `categoryIcon`, `description`, `sortOrder`, `isEnabled`) VALUES
('图片', 'image', 'PictureOutlined', '图片类素材，支持jpg、png、gif、webp等格式的在线URL', 1, 1),
('文本', 'text', 'FileTextOutlined', '文本类素材，如文章内容、产品描述、公司简介等', 2, 1),
('视频', 'video', 'VideoCameraOutlined', '视频类素材，支持mp4、webm等格式的在线URL', 3, 1),
('音频', 'audio', 'SoundOutlined', '音频类素材，支持mp3、wav等格式的在线URL', 4, 1),
('链接', 'link', 'LinkOutlined', '外部链接素材，如参考网站、API文档等', 5, 1);

-- -----------------------------------------------------
-- 素材表
-- -----------------------------------------------------
DROP TABLE IF EXISTS `material`;
CREATE TABLE `material` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `materialName` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '素材名称',
  `categoryId` bigint NOT NULL COMMENT '分类id',
  `materialType` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '素材类型：image/text/video/audio/link',
  `content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '素材内容（URL或文本内容）',
  `thumbnailUrl` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '缩略图URL（图片/视频类素材）',
  `fileSize` bigint DEFAULT NULL COMMENT '文件大小（字节，URL类素材可为空）',
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '素材描述/备注',
  `tags` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标签（逗号分隔）',
  `isPublic` tinyint NOT NULL DEFAULT '0' COMMENT '是否公开（0-私有，1-公开）',
  `useCount` int NOT NULL DEFAULT '0' COMMENT '使用次数',
  `userId` bigint NOT NULL COMMENT '所属用户id',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_userId` (`userId`),
  KEY `idx_categoryId` (`categoryId`),
  KEY `idx_materialType` (`materialType`),
  KEY `idx_isPublic` (`isPublic`),
  KEY `idx_createTime` (`createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='素材表';

-- 插入示例素材数据
INSERT INTO `material` (`materialName`, `categoryId`, `materialType`, `content`, `thumbnailUrl`, `description`, `tags`, `isPublic`, `userId`) VALUES
('示例风景图片', 1, 'image', 'https://picsum.photos/800/600?random=1', 'https://picsum.photos/200/150?random=1', '一张美丽的风景图片，适合用作网站背景或Banner', '风景,自然,背景', 1, 390705603766513664),
('示例人物头像', 1, 'image', 'https://picsum.photos/400/400?random=2', 'https://picsum.photos/100/100?random=2', '人物头像示例，适合用作团队成员展示', '头像,人物,团队', 1, 390705603766513664),
('公司简介文本', 2, 'text', '我们是一家专注于人工智能技术研发的创新型科技公司，致力于为企业提供智能化解决方案。公司成立于2020年，总部位于北京，目前拥有超过100名技术专家和研发人员。我们的核心产品包括智能客服系统、数据分析平台和自动化办公工具等。', NULL, '公司简介示例文本', '公司,简介,关于我们', 1, 390705603766513664),
('产品特性描述', 2, 'text', '【高效便捷】一键生成，快速部署，节省90%的开发时间\n【智能优化】AI驱动的代码优化，自动适配多端设备\n【安全可靠】企业级安全防护，数据加密存储\n【持续更新】定期功能更新，紧跟技术前沿', NULL, '产品特性描述文本', '产品,特性,功能', 1, 390705603766513664);
