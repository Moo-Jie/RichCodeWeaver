package com.rich.ai.rag;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG（检索增强生成）知识库配置属性类
 * 统一管理 RAG 相关的所有配置项，包括：
 * - PGVector 向量数据库连接参数
 * - Embedding 向量模型参数
 * - 知识库文档路径与摄入策略
 *
 * @author DuRuiChi
 * @create 2026/3/26
 **/
@Data
@ConfigurationProperties(prefix = RagAiConstant.RAG_CONFIG_PREFIX)
public class RagProperties {

    /**
     * RAG 功能总开关，设为 false 时不初始化任何 RAG 相关 Bean
     * 关闭后系统仍可正常运行，只是 AI 不会检索知识库
     **/
    private boolean enabled = false;

    /**
     * 是否在应用启动时自动检查并摄入知识库文档
     * 开启后会在首次启动时自动将文档向量化存入 PGVector
     * 已有数据时会跳过，不会重复摄入
     **/
    private boolean autoIngestOnStartup = true;

    /**
     * RAG 知识库文档所在的文件系统路径（绝对路径）
     * 目录下的 .md 文件将被读取、切分、向量化后存入 PGVector
     * 示例：E:/AAA_idea_java/RichCodeWeaver/docs/ragDocs
     **/
    private String docsPath;

    /**
     * PGVector 向量数据库连接配置
     **/
    private PgVectorProperties pgvector = new PgVectorProperties();

    /**
     * Embedding 向量模型配置（使用 OpenAI 兼容接口）
     **/
    private EmbeddingModelProperties embeddingModel = new EmbeddingModelProperties();

    /**
     * PGVector 向量数据库连接参数
     * PostgreSQL 需预先安装 pgvector 扩展：CREATE EXTENSION IF NOT EXISTS vector;
     **/
    @Data
    public static class PgVectorProperties {
        /**
         * PostgreSQL 主机地址
         **/
        private String host = "localhost";

        /**
         * PostgreSQL 端口号
         **/
        private int port = 5432;

        /**
         * 数据库名称
         **/
        private String database;

        /**
         * 数据库用户名
         **/
        private String user;

        /**
         * 数据库密码
         **/
        private String password;

        /**
         * 向量存储表名（PGVector 会自动创建该表）
         **/
        private String table = "rag_embeddings";

        /**
         * 向量维度，必须与 Embedding 模型输出维度一致
         * 常见维度：text-embedding-3-small = 1536, text-embedding-ada-002 = 1536
         **/
        private int dimension = 1536;

        /**
         * 是否启用 IVFFlat 索引以加速相似度检索
         * 数据量较大时（>1000条）建议开启
         **/
        private boolean useIndex = true;

        /**
         * IVFFlat 索引的 list 数量，影响索引的精度和速度
         * 一般设为 sqrt(数据量)，小数据集可保持默认值
         **/
        private int indexListSize = 100;
    }

    /**
     * Embedding 向量模型配置
     * 使用阿里云 DashScope 原生 SDK（非 OpenAI 兼容模式）
     **/
    @Data
    public static class EmbeddingModelProperties {
        /**
         * DashScope API 密钥
         **/
        private String apiKey;

        /**
         * Embedding 模型名称
         * 推荐：text-embedding-v4（通义实验室基于 Qwen3 训练，支持 64~2048 自定义维度）
         **/
        private String modelName = "text-embedding-v4";
    }
}
