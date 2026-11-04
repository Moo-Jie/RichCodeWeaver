package com.rich.ai.config;

import com.rich.common.constant.RagAiConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;

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
public class RagPropertiesConfig {

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
     **/
    private String docsPath;

    private String storeType = "pgvector";

    /**
     * PGVector 向量数据库连接配置
     **/
    private PgVectorProperties pgvector = new PgVectorProperties();

    private MilvusProperties milvus = new MilvusProperties();

    /**
     * Embedding 向量模型配置（使用 OpenAI 兼容接口）
     **/
    private EmbeddingModelProperties embeddingModel = new EmbeddingModelProperties();

    /**
     * Rerank 重排序模型配置
     **/
    private RerankModelProperties rerank = new RerankModelProperties();

    /**
     * Elasticsearch 配置（BM25 关键词检索）
     **/
    private ElasticsearchProperties elasticsearch = new ElasticsearchProperties();

    public boolean useMilvus() {
        return "milvus".equalsIgnoreCase(storeType == null ? "pgvector" : storeType.trim().toLowerCase(Locale.ROOT));
    }

    public int resolveActiveDimension() {
        return useMilvus() ? milvus.getDimension() : pgvector.getDimension();
    }

    /**
     * PGVector 向量数据库连接参数
     * PostgreSQL 需预先安装 pgvector 扩展：CREATE EXTENSION IF NOT EXISTS vector;
     **/
    @Data
    public static class PgVectorProperties {
        /**
         * PostgreSQL 主机地址
         **/
        private String host;

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

    @Data
    public static class MilvusProperties {
        private String host;

        private int port = 19530;

        private String uri;

        private String token;

        private String username;

        private String password;

        private String databaseName;

        private String collectionName = "rag_embeddings";

        private int dimension = 1536;

        private String indexType = "FLAT";

        private String metricType = "COSINE";

        private String consistencyLevel = "EVENTUALLY";

        private boolean retrieveEmbeddingsOnSearch = false;

        private boolean autoFlushOnInsert = true;

        private String idFieldName = "id";

        private String textFieldName = "text";

        private String metadataFieldName = "metadata";

        private String vectorFieldName = "vector";
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

    /**
     * Rerank 重排序模型配置
     * 使用阿里云 DashScope Text-Rerank API
     **/
    @Data
    public static class RerankModelProperties {
        /**
         * Rerank 功能开关，设为 false 时跳过重排序
         **/
        private boolean enabled = false;

        /**
         * Rerank 模型名称
         * 推荐：qwen3-vl-rerank（通义实验室多模态重排序模型）
         **/
        private String modelName = "qwen3-vl-rerank";

        /**
         * DashScope API 密钥（留空时复用 embedding-model 的 api-key）
         **/
        private String apiKey;

        /**
         * 重排序后返回的最大结果数
         * 建议设为 3~5，过多则 Rerank 意义降低
         **/
        private int topN = 3;
    }

    /**
     * Elasticsearch 配置（BM25 关键词检索，混合检索用）
     **/
    @Data
    public static class ElasticsearchProperties {
        /**
         * ES 功能开关，设为 false 时不初始化 ES 客户端，退化为纯向量检索
         **/
        private boolean enabled = false;

        /**
         * ES 连接地址（含协议和端口）
         **/
        private String uris;

        /**
         * ES 用户名（无认证时留空）
         **/
        private String username;

        /**
         * ES 密码（无认证时留空）
         **/
        private String password;

        /**
         * RAG 文档片段索引名称
         **/
        private String indexName = "rag_documents";
    }
}
