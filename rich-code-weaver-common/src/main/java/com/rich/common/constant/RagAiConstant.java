package com.rich.common.constant;

/**
 * AI 模块下 RAG 相关常量
 *
 * @author DuRuiChi
 * @create 2026/4/9
 */
public final class RagAiConstant {

    /**
     * RAG 配置前缀
     */
    public static final String RAG_CONFIG_PREFIX = "rag";

    /**
     * RAG 启用配置项名称
     */
    public static final String RAG_ENABLED_NAME = "enabled";

    /**
     * RAG 启用配置项值
     */
    public static final String RAG_ENABLED_VALUE = "true";

    /**
     * RAG EmbeddingModel Bean 名称
     */
    public static final String RAG_EMBEDDING_MODEL_BEAN = "ragEmbeddingModel";

    /**
     * RAG EmbeddingStore Bean 名称
     */
    public static final String RAG_EMBEDDING_STORE_BEAN = "ragEmbeddingStore";

    /**
     * RAG RerankModel Bean 名称
     */
    public static final String RAG_RERANK_MODEL_BEAN = "ragRerankModel";

    /**
     * RAG Elasticsearch Service Bean 名称
     */
    public static final String RAG_ES_SERVICE_BEAN = "ragElasticsearchService";

    private RagAiConstant() {
    }
}
