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
  `appName` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用名称',
  `cover` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用封面',
  `initPrompt` text COLLATE utf8mb4_unicode_ci COMMENT '应用初始化的 prompt',
  `codeGenType` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '代码生成类型（枚举）',
  `deployKey` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '部署标识',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `app`
--

LOCK TABLES `app` WRITE;
/*!40000 ALTER TABLE `app` DISABLE KEYS */;
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
  `message` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息',
  `messageType` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'user/ai',
  `appId` bigint NOT NULL COMMENT '应用id',
  `userId` bigint NOT NULL COMMENT '创建用户id',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_appId` (`appId`),
  KEY `idx_createTime` (`createTime`),
  KEY `idx_appId_createTime` (`appId`,`createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话历史';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chat_history`
--

LOCK TABLES `chat_history` WRITE;
/*!40000 ALTER TABLE `chat_history` DISABLE KEYS */;
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
  `userAccount` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账号',
  `userPassword` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
  `userName` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户昵称',
  `userAvatar` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户头像',
  `userProfile` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户简介',
  `userRole` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin',
  `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_userAccount` (`userAccount`),
  KEY `idx_userName` (`userName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
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

-- Dump completed on 2025-08-04 19:45:39
