package com.rich.codeweaver.config;

import com.rich.codeweaver.rag.DashScopeTextEmbeddingModel;
import com.rich.codeweaver.rag.RagContentRetrieverAugmentorFactory;
import com.rich.codeweaver.rag.RagDocumentIndexCreationService;
import com.rich.codeweaver.rag.RagProperties;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG（检索增强生成）核心配置类
 * 负责初始化 RAG 所需的基础设施 Bean：
 *
 * @author DuRuiChi
 * @create 2026/3/26
 * @see RagProperties 配置属性定义
 * @see RagDocumentIndexCreationService 文档摄入服务
 * @see RagContentRetrieverAugmentorFactory 检索增强工厂
 **/
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(RagProperties.class)
public class RagConfig {

    /**
     * 创建 Embedding 向量模型 Bean
     * 使用阿里云 DashScope 原生 SDK 调用文本向量模型（text-embedding-v4）
     *
     * @param ragProperties RAG 配置属性，包含 Embedding 模型的 API 密钥、模型名称等
     * @return EmbeddingModel 向量模型实例（DashScope 文本向量适配器）
     * @author DuRuiChi
     * @create 2026/3/27
     **/
    @Bean("ragEmbeddingModel")
    public EmbeddingModel ragEmbeddingModel(RagProperties ragProperties) {
        // 从配置中获取 Embedding 模型参数
        RagProperties.EmbeddingModelProperties embeddingProps = ragProperties.getEmbeddingModel();

        log.info("【RAG】初始化 Embedding 向量模型（DashScope 原生 SDK），模型名称: {}",
                embeddingProps.getModelName());

        // 从 PGVector 配置中获取向量维度，确保 Embedding 模型输出维度与存储维度一致
        int dimension = ragProperties.getPgvector().getDimension();

        // 构建 DashScope 文本向量模型适配器
        // 内部使用 DashScope 原生 TextEmbedding API，支持 text-embedding-v4 批量处理
        EmbeddingModel model = new DashScopeTextEmbeddingModel(
                embeddingProps.getModelName(),   // 模型名称（text-embedding-v4）
                embeddingProps.getApiKey(),       // DashScope API 密钥
                dimension                        // 请求的向量维度（与 PGVector 存储维度一致）
        );

        log.info("【RAG】Embedding 向量模型初始化完成，请求向量维度: {}", dimension);
        return model;
    }

    /**
     * 创建 PGVector 向量存储 Bean
     * 使用 PostgreSQL 的 pgvector 扩展存储和检索文本向量
     *
     * @param ragProperties RAG 配置属性，包含 PGVector 连接参数
     * @return PgVectorEmbeddingStore 向量存储实例
     * @author DuRuiChi
     * @create 2026/3/26
     **/
    @Bean("ragEmbeddingStore")
    public EmbeddingStore<TextSegment> ragEmbeddingStore(RagProperties ragProperties) {
        // 从配置中获取 PGVector 数据库连接参数
        RagProperties.PgVectorProperties pgProps = ragProperties.getPgvector();

        log.info("【RAG】初始化 PGVector 向量存储，数据库: {}:{}/{}，表名: {}，向量维度: {}",
                pgProps.getHost(), pgProps.getPort(), pgProps.getDatabase(),
                pgProps.getTable(), pgProps.getDimension());

        // 构建 PGVector 向量存储实例
        PgVectorEmbeddingStore store = PgVectorEmbeddingStore.builder()
                //  数据库连接参数 
                .host(pgProps.getHost())             // PostgreSQL 主机地址
                .port(pgProps.getPort())              // PostgreSQL 端口
                .database(pgProps.getDatabase())      // 数据库名称
                .user(pgProps.getUser())              // 数据库用户名
                .password(pgProps.getPassword())      // 数据库密码

                //  存储表配置 
                .table(pgProps.getTable())            // 向量存储表名
                .dimension(pgProps.getDimension())    // 向量维度（必须与 Embedding 模型输出维度一致）

                //  表管理策略 
                .createTable(true)                    // 表不存在时自动创建（首次启动时）
                .dropTableFirst(false)                // 不删除已有表（保留历史向量数据）

                //  索引配置 
                .useIndex(pgProps.isUseIndex())       // 是否启用 IVFFlat 索引加速检索
                .indexListSize(pgProps.getIndexListSize()) // 索引 list 数量

                .build();

        log.info("【RAG】PGVector 向量存储初始化完成，表 {} 已就绪", pgProps.getTable());
        return store;
    }
}
