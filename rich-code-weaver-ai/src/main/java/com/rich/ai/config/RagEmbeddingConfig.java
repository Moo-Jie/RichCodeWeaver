package com.rich.ai.config;

import cn.hutool.core.util.StrUtil;
import com.rich.ai.rag.embeddingModel.DashScopeTextEmbeddingModel;
import com.rich.ai.config.chatModel.DashScopeTextRerankModel;
import com.rich.ai.rag.RagContentRetrieverAugmentorFactory;
import com.rich.ai.rag.documentIndex.RagDocumentIndexCreationService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * RAG（检索增强生成）核心配置类
 * 负责初始化 RAG 所需的基础设施 Bean：
 *
 * @author DuRuiChi
 * @create 2026/3/26
 * @see RagPropertiesConfig 配置属性定义
 * @see RagDocumentIndexCreationService 文档摄入服务
 * @see RagContentRetrieverAugmentorFactory 检索增强工厂
 **/
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(RagPropertiesConfig.class)
public class RagEmbeddingConfig {

    /**
     * 创建 Embedding 向量模型 Bean
     * 使用阿里云 DashScope 原生 SDK 调用文本向量模型（text-embedding-v4）
     *
     * @param ragPropertiesConfig RAG 配置属性，包含 Embedding 模型的 API 密钥、模型名称等
     * @return EmbeddingModel 向量模型实例（DashScope 文本向量适配器）
     * @author DuRuiChi
     * @create 2026/3/27
     **/
    @Bean("ragEmbeddingModel")
    public EmbeddingModel ragEmbeddingModel(RagPropertiesConfig ragPropertiesConfig) {
        // 从配置中获取 Embedding 模型参数
        RagPropertiesConfig.EmbeddingModelProperties embeddingProps = ragPropertiesConfig.getEmbeddingModel();

        log.info("【RAG】初始化 Embedding 向量模型（DashScope 原生 SDK），模型名称: {}",
                embeddingProps.getModelName());

        int dimension = ragPropertiesConfig.resolveActiveDimension();

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
     * 创建向量存储 Bean
     * 使用 PostgreSQL 的 pgvector 或 Milvus 存储向量
     *
     * @param ragPropertiesConfig RAG 配置属性，包含 PGVector 连接参数
     * @return PgVectorEmbeddingStore 向量存储实例
     * @author DuRuiChi
     * @create 2026/3/26
     **/
    @Bean("ragEmbeddingStore")
    public EmbeddingStore<TextSegment> ragEmbeddingStore(RagPropertiesConfig ragPropertiesConfig) {
        if (ragPropertiesConfig.useMilvus()) {
            return buildMilvusEmbeddingStore(ragPropertiesConfig.getMilvus());
        }
        return buildPgVectorEmbeddingStore(ragPropertiesConfig.getPgvector());
    }

    /**
     * 创建 Rerank 重排序模型 Bean
     * 使用阿里云 DashScope Text-Rerank API（Qwen3-VL-Rerank）
     * 仅在 rag.rerank.enabled=true 时创建
     *
     * @param ragPropertiesConfig RAG 配置属性，包含 Rerank 模型参数
     * @return DashScopeTextRerankModel 重排序模型实例，未启用时返回 null
     * @author DuRuiChi
     * @create 2026/5/17
     **/
    @Bean("ragRerankModel")
    public DashScopeTextRerankModel ragRerankModel(RagPropertiesConfig ragPropertiesConfig) {
        RagPropertiesConfig.RerankModelProperties rerankProps = ragPropertiesConfig.getRerank();
        if (!rerankProps.isEnabled()) {
            log.info("【RAG】Rerank 重排序功能未启用（rag.rerank.enabled=false）");
            return null;
        }

        // API 密钥：优先使用 rerank 专属密钥，留空时复用 embedding-model 的密钥
        String apiKey = StrUtil.isNotBlank(rerankProps.getApiKey())
                ? rerankProps.getApiKey()
                : ragPropertiesConfig.getEmbeddingModel().getApiKey();

        log.info("【RAG】初始化 Rerank 重排序模型（DashScope），模型名称: {}，topN: {}",
                rerankProps.getModelName(), rerankProps.getTopN());

        DashScopeTextRerankModel model = new DashScopeTextRerankModel(
                rerankProps.getModelName(),
                apiKey,
                rerankProps.getTopN()
        );

        log.info("【RAG】Rerank 重排序模型初始化完成");
        return model;
    }

    private EmbeddingStore<TextSegment> buildPgVectorEmbeddingStore(RagPropertiesConfig.PgVectorProperties pgProps) {
        log.info("【RAG】初始化 PGVector 向量存储，数据库: {}:{}/{}，表名: {}，向量维度: {}",
                pgProps.getHost(), pgProps.getPort(), pgProps.getDatabase(),
                pgProps.getTable(), pgProps.getDimension());

        PgVectorEmbeddingStore store = PgVectorEmbeddingStore.builder()
                .host(pgProps.getHost())
                .port(pgProps.getPort())
                .database(pgProps.getDatabase())
                .user(pgProps.getUser())
                .password(pgProps.getPassword())
                .table(pgProps.getTable())
                .dimension(pgProps.getDimension())
                .createTable(true)
                .dropTableFirst(false)
                .useIndex(pgProps.isUseIndex())
                .indexListSize(pgProps.getIndexListSize())
                .build();

        log.info("【RAG】PGVector 向量存储初始化完成，表 {} 已就绪", pgProps.getTable());
        return store;
    }

    private EmbeddingStore<TextSegment> buildMilvusEmbeddingStore(RagPropertiesConfig.MilvusProperties milvusProps) {
        log.info("【RAG】初始化 Milvus 向量存储，地址: {}:{}，集合名: {}，向量维度: {}",
                milvusProps.getHost(), milvusProps.getPort(), milvusProps.getCollectionName(), milvusProps.getDimension());

        try {
            Class<?> storeClass = Class.forName("dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore");
            Class<?> builderClass = Class.forName("dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore$Builder");
            Object builder = storeClass.getMethod("builder").invoke(null);

            invokeBuilder(builderClass, builder, "host", String.class, milvusProps.getHost());
            invokeBuilder(builderClass, builder, "port", Integer.class, milvusProps.getPort());
            invokeBuilder(builderClass, builder, "collectionName", String.class, milvusProps.getCollectionName());
            invokeBuilder(builderClass, builder, "dimension", Integer.class, milvusProps.getDimension());
            invokeBuilder(builderClass, builder, "indexType", loadMilvusEnum("io.milvus.param.IndexType", milvusProps.getIndexType(), "FLAT"));
            invokeBuilder(builderClass, builder, "metricType", loadMilvusEnum("io.milvus.param.MetricType", milvusProps.getMetricType(), "COSINE"));
            invokeBuilder(builderClass, builder, "consistencyLevel", loadMilvusEnum("io.milvus.common.clientenum.ConsistencyLevelEnum", milvusProps.getConsistencyLevel(), "EVENTUALLY"));
            invokeBuilder(builderClass, builder, "retrieveEmbeddingsOnSearch", Boolean.class, milvusProps.isRetrieveEmbeddingsOnSearch());
            invokeBuilder(builderClass, builder, "autoFlushOnInsert", Boolean.class, milvusProps.isAutoFlushOnInsert());
            invokeBuilder(builderClass, builder, "idFieldName", String.class, milvusProps.getIdFieldName());
            invokeBuilder(builderClass, builder, "textFieldName", String.class, milvusProps.getTextFieldName());
            invokeBuilder(builderClass, builder, "metadataFieldName", String.class, milvusProps.getMetadataFieldName());
            invokeBuilder(builderClass, builder, "vectorFieldName", String.class, milvusProps.getVectorFieldName());

            invokeBuilderIfNotBlank(builderClass, builder, "uri", milvusProps.getUri());
            invokeBuilderIfNotBlank(builderClass, builder, "token", milvusProps.getToken());
            invokeBuilderIfNotBlank(builderClass, builder, "username", milvusProps.getUsername());
            invokeBuilderIfNotBlank(builderClass, builder, "password", milvusProps.getPassword());
            invokeBuilderIfNotBlank(builderClass, builder, "databaseName", milvusProps.getDatabaseName());

            @SuppressWarnings("unchecked")
            EmbeddingStore<TextSegment> store = (EmbeddingStore<TextSegment>) builderClass.getMethod("build").invoke(builder);
            log.info("【RAG】Milvus 向量存储初始化完成，集合 {} 已就绪", milvusProps.getCollectionName());
            return store;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Milvus 相关依赖未加载到运行时 classpath，请检查 langchain4j-milvus 与 milvus-sdk-java 依赖", e);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("Milvus 向量存储初始化失败，请检查 Milvus SDK 版本与 LangChain4j 兼容性", e);
        }
    }

    private Object loadMilvusEnum(String className, String configuredValue, String defaultValue) {
        try {
            Class<?> enumClass = Class.forName(className);
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object enumValue = Enum.valueOf((Class<? extends Enum>) enumClass.asSubclass(Enum.class),
                    normalizeEnumValue(configuredValue, defaultValue));
            return enumValue;
        } catch (Exception e) {
            try {
                Class<?> enumClass = Class.forName(className);
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object enumValue = Enum.valueOf((Class<? extends Enum>) enumClass.asSubclass(Enum.class), defaultValue);
                log.warn("【RAG】Milvus 枚举配置无效: class={}, value={}，已回退为 {}", className, configuredValue, defaultValue);
                return enumValue;
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("Milvus SDK 缺少枚举类型: " + className, ex);
            }
        }
    }

    private void invokeBuilderIfNotBlank(Class<?> builderClass, Object builder, String methodName, String value)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (StrUtil.isNotBlank(value)) {
            invokeBuilder(builderClass, builder, methodName, String.class, value);
        }
    }

    private void invokeBuilder(Class<?> builderClass, Object builder, String methodName, Class<?> parameterType, Object value)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = builderClass.getMethod(methodName, parameterType);
        method.invoke(builder, value);
    }

    private void invokeBuilder(Class<?> builderClass, Object builder, String methodName, Object value)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method targetMethod = null;
        for (Method method : builderClass.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                targetMethod = method;
                break;
            }
        }
        if (targetMethod == null) {
            throw new NoSuchMethodException("未找到 Milvus Builder 方法: " + methodName);
        }
        targetMethod.invoke(builder, value);
    }

    private String normalizeEnumValue(String value, String defaultValue) {
        if (StrUtil.isBlank(value)) {
            return defaultValue;
        }
        return value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }
}
