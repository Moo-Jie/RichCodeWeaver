package com.rich.ai.rag;

import com.rich.ai.config.RagConfig;
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
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class RagContentRetrieverAugmentorFactory {

    /**
     * 兜底提示词模板（当数据库无配置时使用）
     **/
    private static final String DEFAULT_INJECTION_TEMPLATE =
            "{{userMessage}}\n\n" +
                    "---\n" +
                    "【权威参考知识】以下内容来自系统知识库，是你生成代码时必须严格遵循的开发规范与约束，不可违反或忽略：\n\n" +
                    "{{contents}}\n" +
                    "---";

    /**
     * RAG 参数提供者（动态从数据库加载参数，可选注入）
     **/
    @Autowired(required = false)
    private RagParamProvider ragParamProvider;

    /**
     * Embedding 向量模型，检索阶段用于将用户查询向量化
     **/
    @Resource(name = "ragEmbeddingModel")
    private EmbeddingModel embeddingModel;

    /**
     * PGVector 向量存储，检索阶段从中搜索相似文档片段
     **/
    @Resource(name = "ragEmbeddingStore")
    private EmbeddingStore<TextSegment> embeddingStore;

    /**
     * Agent 模式专用的 codeGenType 标识
     * Agent 模式下检索此类型的 RAG 文档，与工作流模式的类型完全隔离
     */
    public static final String AGENT_CODE_GEN_TYPE = "AGENT";

    /**
     * 通用类型标识，表示该文档适用于所有代码生成类型
     */
    private static final String GENERAL_CODE_GEN_TYPE = "GENERAL";

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
        log.info("【RAG 检索增强-工作流】为代码生成类型 {} 创建 RetrievalAugmentor", codeGenTypeName);

        // 工作流模式：检索指定类型 + GENERAL 类型的文档
        RetrievalAugmentor augmentor = buildRetrievalAugmentor(
                metadataKey("bizType").isEqualTo(CODE_GEN_BIZ_TYPE)
                        .and(metadataKey("codeGenType").isEqualTo(codeGenTypeName)
                                .or(metadataKey("codeGenType").isEqualTo(GENERAL_CODE_GEN_TYPE)))
        );

        log.info("【RAG 检索增强-工作流】RetrievalAugmentor 创建完成，codeGenType={}", codeGenTypeName);
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
        log.info("【RAG 检索增强-Agent】为 Agent 模式创建 RetrievalAugmentor");

        // Agent 模式：只检索 AGENT 类型的文档
        RetrievalAugmentor augmentor = buildRetrievalAugmentor(
                metadataKey("bizType").isEqualTo(CODE_GEN_BIZ_TYPE)
                        .and(metadataKey("codeGenType").isEqualTo(AGENT_CODE_GEN_TYPE))
        );

        log.info("【RAG 检索增强-Agent】RetrievalAugmentor 创建完成，codeGenType={}", AGENT_CODE_GEN_TYPE);
        return augmentor;
    }

    /**
     * 为 AI 客服创建 RetrievalAugmentor
     * 只检索 bizType=CUSTOMER_SERVICE 的知识库
     *
     * @return RetrievalAugmentor 检索增强器实例
     */
    public RetrievalAugmentor createCustomerServiceRetrievalAugmentor() {
        log.info("【RAG 检索增强-客服】为 AI 客服创建 RetrievalAugmentor");
        RetrievalAugmentor augmentor = buildRetrievalAugmentor(
                metadataKey("bizType").isEqualTo(CUSTOMER_SERVICE_BIZ_TYPE)
        );
        log.info("【RAG 检索增强-客服】RetrievalAugmentor 创建完成，bizType={}", CUSTOMER_SERVICE_BIZ_TYPE);
        return augmentor;
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

        // 步骤1：加载 RAG 参数（从数据库或使用默认值）
        int maxResults = ragParamProvider != null ? ragParamProvider.getMaxResults() : 5;
        double minScore = ragParamProvider != null ? ragParamProvider.getMinScore() : 0.6;
        String injectionTemplate = ragParamProvider != null
                ? ragParamProvider.getInjectionPromptTemplate() : DEFAULT_INJECTION_TEMPLATE;

        // 步骤2：构建 ContentRetriever（内容检索器）
        // 基于 PGVector 的向量相似度检索，附加 codeGenType 元数据过滤
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)       // 向量存储（PGVector）
                .embeddingModel(embeddingModel)       // 向量模型（用于查询向量化）
                .maxResults(maxResults)               // 最大检索结果数
                .minScore(minScore)                   // 最低相似度阈值
                .filter(metadataFilter)               // 元数据过滤器（按 codeGenType 过滤）
                .build();

        // 步骤3：构建 ContentInjector（内容注入器）
        // 使用自定义提示词模板，将检索到的知识库内容以权威参考形式注入用户消息
        ContentInjector contentInjector = DefaultContentInjector.builder()
                .promptTemplate(PromptTemplate.from(injectionTemplate))  // 注入模板
                .metadataKeysToInclude(List.of("source", "title"))      // 包含的元数据字段
                .build();

        // 步骤4：组装 RetrievalAugmentor（检索增强器）
        // DefaultRetrievalAugmentor 封装了完整的检索增强管道：
        //   用户查询 → ContentRetriever 检索 → ContentInjector 注入 → 增强后的消息
        return DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)   // 内容检索器
                .contentInjector(contentInjector)     // 内容注入器
                .build();
    }
}
