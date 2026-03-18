-- MySQL dump 10.13  Distrib 8.0.35, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: rich_code_weaver
-- ------------------------------------------------------
-- Server version	8.0.35

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `app`
--

DROP TABLE IF EXISTS `app`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `app` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `appName` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用名称',
  `cover` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用封面',
  `initPrompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '应用初始化的 prompt',
  `codeGenType` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '代码生成类型（枚举）',
  `deployKey` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '部署标识',
  `deployedTime` datetime DEFAULT NULL COMMENT '部署时间',
  `priority` int NOT NULL DEFAULT '0' COMMENT '优先级',
  `userId` bigint NOT NULL COMMENT '创建用户id',
  `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_deployKey` (`deployKey`),
  KEY `idx_appName` (`appName`),
  KEY `idx_userId` (`userId`)
) ENGINE=InnoDB AUTO_INCREMENT=391758088325152769 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `app`
--

LOCK TABLES `app` WRITE;
/*!40000 ALTER TABLE `app` DISABLE KEYS */;
INSERT INTO `app` VALUES (391758088325152768,'制作前端知识学习单页应用，采用卡片式布局展示HTML、CSS','https://rich-tams.oss-cn-beijing.aliyuncs.com/logo.png','制作前端知识学习单页应用，采用卡片式布局展示HTML、CSS、JavaScript的示例和说明，包含代码高亮和实时预览功能。','single_html',NULL,NULL,0,390705603766513664,'2026-03-18 14:07:46','2026-03-18 14:07:46','2026-03-18 14:07:46',0);
/*!40000 ALTER TABLE `app` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `chat_history`
--

DROP TABLE IF EXISTS `chat_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息',
  `messageType` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'user/ai',
  `appId` bigint NOT NULL COMMENT '应用id',
  `userId` bigint NOT NULL COMMENT '创建用户id',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_appId` (`appId`),
  KEY `idx_createTime` (`createTime`),
  KEY `idx_appId_createTime` (`appId`,`createTime`)
) ENGINE=InnoDB AUTO_INCREMENT=391758269984653313 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话历史';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chat_history`
--

LOCK TABLES `chat_history` WRITE;
/*!40000 ALTER TABLE `chat_history` DISABLE KEYS */;
INSERT INTO `chat_history` VALUES (391758088987852800,'制作前端知识学习单页应用，采用卡片式布局展示HTML、CSS、JavaScript的示例和说明，包含代码高亮和实时预览功能。','user',391758088325152768,390705603766513664,'2026-03-18 14:07:46','2026-03-18 14:07:46',0),(391758269942710272,'\n\n# 代码生成 Agent 启动中...\n\n**应用ID:** 391758088325152768\n\n**生成类型:** single_html\n\n**原始需求:** 制作前端知识学习单页应用，采用卡片式布局展示HTML、CSS、JavaScript的示例和说明，包含代码高亮和实时预览功能。\n\n\n\n## 🎬 开始执行规划节点\n\n\n\n### ✅ 执行步骤: 工作流初始化\n\n\n\n### ✅ 执行步骤: 网络资源整理已完成\n\n- 整理网络资源: 1 字符\n\n\n### ✅ 执行步骤: 图片收集已完成\n\n\n\n### ✅ 执行步骤: 提示词增强已完成\n\n- 提示词已增强 (长度: 244 字符)\n\n\n### ✅ 执行步骤: 代码生成类型策略已完成\n\n\n\n### ✅ 执行步骤: AI 代码生成已完成\n\n- 代码输出目录: E:\\AAA_idea_java\\RichCodeWeaver/appCode/code_output/single_html_391758088325152768\n\n**等待 AI 代码审查...**\n\n\n### ✅ 执行步骤: 代码审查\n\n- 审查结果: ✅ 通过\n\n\n### ✅ 执行步骤: 代码审查\n\n\n\n# 🎉 代码生成任务全部完成！\n\n','ai',391758088325152768,390705603766513664,'2026-03-18 14:08:30','2026-03-18 14:08:30',0);
/*!40000 ALTER TABLE `chat_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `userAccount` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账号',
  `userPassword` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
  `userName` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户昵称',
  `userAvatar` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户头像',
  `userProfile` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户简介',
  `userRole` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin',
  `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_userAccount` (`userAccount`),
  KEY `idx_userName` (`userName`)
) ENGINE=InnoDB AUTO_INCREMENT=390705603766513665 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (310192815309332480,'user01','9e73f8f59706793106601290f307aaeb','普通用户001','https://rich-tams.oss-cn-beijing.aliyuncs.com/LOGO.jpg','普通用户，用于测试普通权限下的项目功能。','user','2025-08-05 12:16:30','2025-08-05 12:16:30','2025-08-23 14:47:29',0),(310192815309332481,'MOJIE','d6a877d493087b2fef665c5902bc1481','管理员用户','https://rich-tams.oss-cn-beijing.aliyuncs.com/RichInterview/68287d91edb5c5b7e1d9ad29.png','管理员用户，用于测试管理员权限下的项目功能','admin','2025-08-08 14:07:01','2025-08-08 14:07:03','2025-08-23 15:17:34',0),(390705603766513664,'admin','a18e78233df17622f4f90938936ac95c','管理员','https://rich-tams.oss-cn-beijing.aliyuncs.com/LOGO.jpg','管理员用户，用于测试管理员权限下的项目功能','admin','2026-03-15 16:25:34','2026-03-15 16:25:34','2026-03-15 16:25:56',0);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-18 14:15:43
