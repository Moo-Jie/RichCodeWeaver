package com.rich.ai.rag;

import com.rich.ai.config.RagConfig;
import com.rich.common.constant.RagConstant;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.rich.model.enums.RagDocumentBizTypeEnum.CODE_GEN;
import static com.rich.model.enums.RagDocumentBizTypeEnum.CUSTOMER_SERVICE;
import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * RAG 知识库 内容检索增强器工厂
 * 负责 ：文档过滤和检索 + 查询增强和关联
 *
 * @author DuRuiChi
 * @create 2026/3/26
 * @see RagConfig RAG 核心配置（提供 EmbeddingModel 和 EmbeddingStore）
 * @see RagDocumentIndexCreationService 文档摄入服务（索引阶段）
 **/
@Slf4j
@Component
@ConditionalOnProperty(prefix = RagAiConstant.RAG_CONFIG_PREFIX,
        name = RagAiConstant.RAG_ENABLED_NAME,
        havingValue = RagAiConstant.RAG_ENABLED_VALUE)
public class RagContentRetrieverAugmentorFactory {

    /**
     * 工作流检索日志标识
     */
    private static final String WORKFLOW_RETRIEVAL_LOG_TAG = "RAG 检索增强-工作流";

    /**
     * Agent 检索日志标识
     */
    private static final String AGENT_RETRIEVAL_LOG_TAG = "RAG 检索增强-Agent";

    /**
     * 客服检索日志标识
     */
    private static final String CUSTOMER_SERVICE_RETRIEVAL_LOG_TAG = "RAG 检索增强-客服";

    /**
     * 客服命中日志标识
     */
    private static final String CUSTOMER_SERVICE_HIT_LOG_TAG = "RAG 检索-客服";

    /**
     * 兜底提示词模板（当数据库无配置时使用）
     **/
    private static final String DEFAULT_INJECTION_TEMPLATE =
            RagConstant.TEMPLATE_USER_MESSAGE_PLACEHOLDER + "\n\n" +
                    "---\n" +
                    "【权威参考知识】以下内容来自系统知识库，是你生成代码时必须严格遵循的开发规范与约束，不可违反或忽略：\n\n" +
                    RagConstant.TEMPLATE_CONTENTS_PLACEHOLDER + "\n" +
                    "---";

    /**
     * RAG 参数提供者（动态从数据库加载参数，可选注入）
     **/
    @Autowired(required = false)
    private RagParamProvider ragParamProvider;

    /**
     * Embedding 向量模型，检索阶段用于将用户查询向量化
     **/
    @Resource(name = RagAiConstant.RAG_EMBEDDING_MODEL_BEAN)
    private EmbeddingModel embeddingModel;

    /**
     * PGVector 向量存储，检索阶段从中搜索相似文档片段
     **/
    @Resource(name = RagAiConstant.RAG_EMBEDDING_STORE_BEAN)
    private EmbeddingStore<TextSegment> embeddingStore;

    /**
     * Agent 模式专用的 codeGenType 标识
     * Agent 模式下检索此类型的 RAG 文档，与工作流模式的类型完全隔离
     */
    public static final String AGENT_CODE_GEN_TYPE = "AGENT";

    /**
     * 通用类型标识，表示该文档适用于所有代码生成类型
     */
    private static final String GENERAL_CODE_GEN_TYPE = RagConstant.DEFAULT_CODE_GEN_TYPE;

    /**
     * AI 客服业务类型标识
     */
    private static final String CUSTOMER_SERVICE_BIZ_TYPE = CUSTOMER_SERVICE.getValue();

    /**
     * 代码生成业务类型标识
     */
    private static final String CODE_GEN_BIZ_TYPE = CODE_GEN.getValue();

    /**
     * 为工作流模式创建 RetrievalAugmentor（检索增强器）
     * 工作流模式的 RAG 过滤策略：
     * - 检索与当前 codeGenType 匹配的文档（HTML/MULTI_FILE/VUE_PROJECT）
     * - 同时检索 GENERAL 类型的通用文档
     * - 不检索 AGENT 类型的文档，实现与 Agent 模式的完全隔离
     *
     * @param codeGenTypeName 代码生成类型（HTML/MULTI_FILE/VUE_PROJECT）
     * @return RetrievalAugmentor 检索增强器实例
     * @author DuRuiChi
     * @create 2026/3/26
     **/
    public RetrievalAugmentor createWorkflowRetrievalAugmentor(String codeGenTypeName) {
        log.info("【{}】为代码生成类型 {} 创建 RetrievalAugmentor", WORKFLOW_RETRIEVAL_LOG_TAG, codeGenTypeName);

        // 工作流模式：检索指定类型 + GENERAL 类型的文档
        RetrievalAugmentor augmentor = buildRetrievalAugmentor(buildWorkflowMetadataFilter(codeGenTypeName));

        log.info("【{}】RetrievalAugmentor 创建完成，codeGenType={}", WORKFLOW_RETRIEVAL_LOG_TAG, codeGenTypeName);
        return augmentor;
    }

    /**
     * 为 Agent 模式创建 RetrievalAugmentor（检索增强器）
     * Agent 模式的 RAG 过滤策略：
     * - 只检索 codeGenType=AGENT 的专用文档
     * - 不检索 GENERAL 类型的文档，避免与工作流模式的通用规范混淆
     * - 与工作流模式的 RAG 完全隔离，互不干扰
     *
     * @return RetrievalAugmentor 检索增强器实例
     * @author DuRuiChi
     * @create 2026/3/29
     **/
    public RetrievalAugmentor createAgentRetrievalAugmentor() {
        log.info("【{}】为 Agent 模式创建 RetrievalAugmentor", AGENT_RETRIEVAL_LOG_TAG);

        // Agent 模式：只检索 AGENT 类型的文档
        RetrievalAugmentor augmentor = buildRetrievalAugmentor(buildAgentMetadataFilter());

        log.info("【{}】RetrievalAugmentor 创建完成，codeGenType={}", AGENT_RETRIEVAL_LOG_TAG, AGENT_CODE_GEN_TYPE);
        return augmentor;
    }

    /**
     * 为 AI 客服创建 RetrievalAugmentor
     * 只检索 bizType=CUSTOMER_SERVICE 的知识库
     *
     * @return RetrievalAugmentor 检索增强器实例
     */
    public RetrievalAugmentor createCustomerServiceRetrievalAugmentor() {
        log.info("【{}】为 AI 客服创建 RetrievalAugmentor", CUSTOMER_SERVICE_RETRIEVAL_LOG_TAG);
        RetrievalAugmentor augmentor = buildRetrievalAugmentor(
                buildCustomerServiceMetadataFilter(),
                CUSTOMER_SERVICE_HIT_LOG_TAG
        );
        log.info("【{}】RetrievalAugmentor 创建完成，bizType={}", CUSTOMER_SERVICE_RETRIEVAL_LOG_TAG, CUSTOMER_SERVICE_BIZ_TYPE);
        return augmentor;
    }

    /**
     * 构建工作流模式的元数据过滤器
     *
     * @param codeGenTypeName 代码生成类型
     * @return 元数据过滤器
     */
    private Filter buildWorkflowMetadataFilter(String codeGenTypeName) {
        return metadataKey(RagConstant.METADATA_BIZ_TYPE).isEqualTo(CODE_GEN_BIZ_TYPE)
                .and(metadataKey(RagConstant.METADATA_CODE_GEN_TYPE).isEqualTo(codeGenTypeName)
                        .or(metadataKey(RagConstant.METADATA_CODE_GEN_TYPE).isEqualTo(GENERAL_CODE_GEN_TYPE)));
    }

    /**
     * 构建 Agent 模式的元数据过滤器
     *
     * @return 元数据过滤器
     */
    private Filter buildAgentMetadataFilter() {
        return metadataKey(RagConstant.METADATA_BIZ_TYPE).isEqualTo(CODE_GEN_BIZ_TYPE)
                .and(metadataKey(RagConstant.METADATA_CODE_GEN_TYPE).isEqualTo(AGENT_CODE_GEN_TYPE));
    }

    /**
     * 构建客服模式的元数据过滤器
     *
     * @return 元数据过滤器
     */
    private Filter buildCustomerServiceMetadataFilter() {
        return metadataKey(RagConstant.METADATA_BIZ_TYPE).isEqualTo(CUSTOMER_SERVICE_BIZ_TYPE);
    }

    /**
     * 构建 RetrievalAugmentor（检索增强器）的通用方法
     * 该方法封装了 RAG 检索增强器的完整构建流程：
     * 1. 从数据库或默认值加载 RAG 参数（maxResults、minScore、injectionTemplate）
     * 2. 构建 ContentRetriever（内容检索器）：基于向量相似度检索，附加元数据过滤
     * 3. 构建 ContentInjector（内容注入器）：将检索到的内容注入到用户消息中
     * 4. 组装 RetrievalAugmentor：封装完整的检索增强管道
     *
     * @param metadataFilter 元数据过滤器，用于按 codeGenType 过滤文档
     * @return RetrievalAugmentor 检索增强器实例
     */
    private RetrievalAugmentor buildRetrievalAugmentor(Filter metadataFilter) {
        return buildRetrievalAugmentor(metadataFilter, null);
    }

    /**
     * 构建 RetrievalAugmentor（可选附带检索日志）
     *
     * @param metadataFilter 元数据过滤器
     * @param retrieverLogTag 检索日志标识
     * @return RetrievalAugmentor 检索增强器实例
     */
    private RetrievalAugmentor buildRetrievalAugmentor(Filter metadataFilter, String retrieverLogTag) {
        // 1：加载 RAG 参数（从数据库或使用默认值）
        int maxResults = getMaxResults();
        double minScore = getMinScore();
        String injectionTemplate = getInjectionTemplate();

        // 2：构建 ContentRetriever（内容检索器）
        // 基于 PGVector 的向量相似度检索，附加 codeGenType 元数据过滤
        ContentRetriever delegateContentRetriever = buildDelegateContentRetriever(metadataFilter, maxResults, minScore);
        ContentRetriever contentRetriever = wrapContentRetrieverWithLog(delegateContentRetriever, retrieverLogTag);

        // 3：构建 ContentInjector（内容注入器）
        // 使用自定义提示词模板，将检索到的知识库内容以权威参考形式注入用户消息
        ContentInjector contentInjector = buildContentInjector(injectionTemplate);

        // 4：组装 RetrievalAugmentor（检索增强器）
        // DefaultRetrievalAugmentor 封装了完整的检索增强管道：
        //   用户查询 → ContentRetriever 检索 → ContentInjector 注入 → 增强后的消息
        return DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)   // 内容检索器
                .contentInjector(contentInjector)     // 内容注入器
                .build();
    }

    /**
     * 获取最大检索结果数
     *
     * @return 最大检索结果数
     */
    private int getMaxResults() {
        return ragParamProvider != null ? ragParamProvider.getMaxResults() : RagConstant.DEFAULT_MAX_RESULTS;
    }

    /**
     * 获取最低相似度阈值
     *
     * @return 最低相似度阈值
     */
    private double getMinScore() {
        return ragParamProvider != null ? ragParamProvider.getMinScore() : RagConstant.DEFAULT_MIN_SCORE;
    }

    /**
     * 获取内容注入模板
     *
     * @return 注入模板
     */
    private String getInjectionTemplate() {
        return ragParamProvider != null ? ragParamProvider.getInjectionPromptTemplate() : DEFAULT_INJECTION_TEMPLATE;
    }

    /**
     * 构建基础内容检索器
     *
     * @param metadataFilter 元数据过滤器
     * @param maxResults 最大返回数量
     * @param minScore 最低分数
     * @return 内容检索器
     */
    private ContentRetriever buildDelegateContentRetriever(Filter metadataFilter, int maxResults, double minScore) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(maxResults)
                .minScore(minScore)
                .filter(metadataFilter)
                .build();
    }

    /**
     * 根据日志标识包装检索器
     * 没有日志标识时直接返回原检索器
     *
     * @param delegateContentRetriever 原始检索器
     * @param retrieverLogTag 日志标识
     * @return 包装后的检索器
     */
    private ContentRetriever wrapContentRetrieverWithLog(ContentRetriever delegateContentRetriever, String retrieverLogTag) {
        if (retrieverLogTag == null || retrieverLogTag.isBlank()) {
            return delegateContentRetriever;
        }
        return query -> {
            var contents = delegateContentRetriever.retrieve(query);
            log.info("【{}】已执行知识库检索，命中 {} 条内容", retrieverLogTag, contents.size());
            return contents;
        };
    }

    /**
     * 构建内容注入器
     *
     * @param injectionTemplate 注入模板
     * @return 内容注入器
     */
    private ContentInjector buildContentInjector(String injectionTemplate) {
        return DefaultContentInjector.builder()
                .promptTemplate(PromptTemplate.from(injectionTemplate))
                .metadataKeysToInclude(List.of(RagConstant.METADATA_SOURCE, RagConstant.METADATA_TITLE))
                .build();
    }
}
