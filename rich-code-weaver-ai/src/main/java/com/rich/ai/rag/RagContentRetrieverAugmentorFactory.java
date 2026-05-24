package com.rich.ai.rag;

import com.rich.ai.config.RagEmbeddingConfig;
import com.rich.ai.rag.documentIndex.RagDocumentIndexCreationService;
import com.rich.ai.config.chatModel.DashScopeTextRerankModel;
import com.rich.ai.service.RagElasticsearchService;
import com.rich.common.constant.RagAiConstant;
import com.rich.common.constant.RagConstant;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.rich.model.enums.RagDocumentBizTypeEnum.CODE_GEN;
import static com.rich.model.enums.RagDocumentBizTypeEnum.CUSTOMER_SERVICE;
import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * RAG 知识库 内容检索增强器工厂
 * 负责 ：文档过滤和检索 + 查询增强和关联
 *
 * @author DuRuiChi
 * @create 2026/3/26
 * @see RagEmbeddingConfig RAG 核心配置（提供 EmbeddingModel 和 EmbeddingStore）
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
     * Rerank 重排序模型，检索阶段对结果进行语义重排序（可选注入）
     **/
    @Autowired(required = false)
    private DashScopeTextRerankModel rerankModel;

    /**
     * Elasticsearch BM25 检索服务（混合检索用，可选注入）
     * 为 null 时退化为纯向量检索
     **/
    @Autowired(required = false)
    private RagElasticsearchService esService;

    /**
     * 混合检索过滤条件（用于 ES BM25 检索的元数据过滤）
     *
     * @param bizType      业务类型（CODE_GEN / CUSTOMER_SERVICE）
     * @param codeGenTypes 代码生成类型列表（为 null 时不过滤）
     */
    private record HybridSearchFilter(String bizType, List<String> codeGenTypes) {
    }

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
        // 分别创建向量元信息、ES 文档元信息
        HybridSearchFilter esFilter = new HybridSearchFilter(
                CODE_GEN_BIZ_TYPE, List.of(codeGenTypeName, GENERAL_CODE_GEN_TYPE));
        RetrievalAugmentor augmentor = buildRetrievalAugmentor(
                buildWorkflowMetadataFilter(codeGenTypeName), null, esFilter);

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
        // 分别创建向量元信息、ES 文档元信息
        HybridSearchFilter esFilter = new HybridSearchFilter(
                CODE_GEN_BIZ_TYPE, List.of(AGENT_CODE_GEN_TYPE));
        RetrievalAugmentor augmentor = buildRetrievalAugmentor(
                buildAgentMetadataFilter(), null, esFilter);

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
        // 分别创建向量元信息、ES 文档元信息
        HybridSearchFilter esFilter = new HybridSearchFilter(
                CUSTOMER_SERVICE_BIZ_TYPE, null);
        RetrievalAugmentor augmentor = buildRetrievalAugmentor(
                buildCustomerServiceMetadataFilter(), CUSTOMER_SERVICE_HIT_LOG_TAG, esFilter);
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
     * 3. 封装混合检索逻辑（向量 + BM25 关键词 → 去重 → RRF 融合 → Rerank）
     * 4. 构建 ContentInjector（内容注入器）：将检索到的内容注入到用户消息中
     * 5. 组装 RetrievalAugmentor：封装完整的检索增强管道
     *
     * @param metadataFilter  元数据过滤器，用于按 codeGenType 过滤文档
     * @param retrieverLogTag 检索日志标识（为 null 时使用默认标识）
     * @param esFilter        混合检索过滤条件（用于 ES BM25 检索）
     * @return RetrievalAugmentor 检索增强器实例
     */
    private RetrievalAugmentor buildRetrievalAugmentor(Filter metadataFilter, String retrieverLogTag,
                                                       HybridSearchFilter esFilter) {
        // 1：加载 RAG 参数（从数据库或使用默认值）
        int maxResults = getMaxResults();
        double minScore = getMinScore();
        String injectionTemplate = getInjectionTemplate();

        // 2：构建 ContentRetriever（内容检索器）,用于向量相似度检索
        // 创建默认内容检索器，附加 codeGenType 元数据过滤
        ContentRetriever defaultContentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(maxResults)
                .minScore(minScore)
                .filter(metadataFilter)
                .build();
        // 自定义创建默认内容检索器，封装混合检索、Rerank、日志输出等逻辑
        ContentRetriever contentRetriever = wrapContentRetrieverWithLog(
                defaultContentRetriever, retrieverLogTag, esFilter);

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
     * 获取向量检索权重
     *
     * @return 向量检索权重
     */
    private double getVectorWeight() {
        return ragParamProvider != null ? ragParamProvider.getVectorWeight() : RagConstant.DEFAULT_VECTOR_WEIGHT;
    }

    /**
     * 获取关键词检索权重
     *
     * @return 关键词检索权重
     */
    private double getKeywordWeight() {
        return ragParamProvider != null ? ragParamProvider.getKeywordWeight() : RagConstant.DEFAULT_KEYWORD_WEIGHT;
    }

    /**
     * 自定义内容检索器：混合检索 + Rerank 重排序 + 日志输出
     * 核心流程：
     * 1. 并行执行：向量相似度检索 + ES BM25 关键词检索
     * 2. 去重 + RRF（Reciprocal Rank Fusion）倒数排名融合
     * 3. Rerank 精排（DashScope 重排序模型）
     * 4. 返回最终结果
     *
     * 降级策略：ES 不可用或异常时，自动退化为纯向量检索
     *
     * @param delegateContentRetriever 原始向量检索器
     * @param retrieverLogTag          日志标识
     * @param esFilter                 ES BM25 检索过滤条件
     * @return 包装后的混合检索器
     * @author DuRuiChi
     * @create 2026/5/19
     */
    private ContentRetriever wrapContentRetrieverWithLog(ContentRetriever delegateContentRetriever,
                                                         String retrieverLogTag,
                                                         HybridSearchFilter esFilter) {
        String logTag = (retrieverLogTag != null && !retrieverLogTag.isBlank())
                ? retrieverLogTag : "RAG 混合检索";
        boolean hasRerank = rerankModel != null;
        // 检查 ES 服务是否成功加载
        boolean hasEs = esService != null && esService.isAvailable();

        // lambda 表达式创建 ContentRetriever 实现类，实现自定义 retrieve 方法
        return query -> {
            List<Content> finalContents;

            // 1.执行混合检索
            if (hasEs) {
                // 混合检索模式：向量 + BM25 并行
                finalContents = executeHybridRetrieval(
                        delegateContentRetriever, query, esFilter, logTag);
            } else {
                // 降级逻辑：纯向量检索模式
                finalContents = delegateContentRetriever.retrieve(query);
                log.info("【{}】纯向量检索命中 {} 条: {}", logTag, finalContents.size(),
                        formatContentBrief(finalContents));
            }

            // 2.Rerank 重排序
            if (hasRerank && !finalContents.isEmpty()) {
                finalContents = rerankModel.rerank(query.text(), finalContents);
                log.info("【{}】Rerank 重排序完成，保留 {} 条: {}", logTag, finalContents.size(),
                        formatContentBrief(finalContents));
            }

            return finalContents;
        };
    }

    /**
     * 执行混合检索：向量相似度 + ES BM25 关键词并行检索，去重后 RRF 融合
     *
     * @param vectorRetriever 向量检索器
     * @param query           用户查询
     * @param esFilter        ES BM25 过滤条件
     * @param logTag          日志标识
     * @return RRF 融合后的内容列表
     */
    private List<Content> executeHybridRetrieval(ContentRetriever vectorRetriever,
                                                  Query query,
                                                  HybridSearchFilter esFilter,
                                                  String logTag) {
        int maxResults = getMaxResults();

        // 1. 并行执行向量相似度检索和 BM25 关键词检索
        CompletableFuture<List<Content>> vectorFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return vectorRetriever.retrieve(query);
            } catch (Exception e) {
                log.error("【{}】向量检索异常: {}", logTag, e.getMessage());
                return Collections.<Content>emptyList();
            }
        });

        CompletableFuture<List<Content>> keywordFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return esService.searchBm25(
                        query.text(),
                        esFilter != null ? esFilter.bizType() : null,
                        esFilter != null ? esFilter.codeGenTypes() : null,
                        maxResults);
            } catch (Exception e) {
                log.warn("【{}】BM25 关键词检索异常，降级忽略: {}", logTag, e.getMessage());
                return Collections.<Content>emptyList();
            }
        });

        // 2. 等待两路检索完成
        List<Content> vectorResults = vectorFuture.join();
        List<Content> keywordResults = keywordFuture.join();

        log.info("【{}】向量检索命中 {} 条: {}", logTag, vectorResults.size(),
                formatContentBrief(vectorResults));
        log.info("【{}】BM25 关键词检索命中 {} 条: {}", logTag, keywordResults.size(),
                formatContentBrief(keywordResults));

        // 3. 去重 + RRF 倒数排名融合
        if (keywordResults.isEmpty()) {
            return vectorResults;
        }

        double vectorWeight = getVectorWeight();
        double keywordWeight = getKeywordWeight();
        List<Content> fusedResults = rrfFuse(vectorResults, keywordResults, vectorWeight, keywordWeight);
        log.info("【{}】RRF 融合完成（向量权重={}, 关键词权重={}, K={}），融合后 {} 条: {}",
                logTag, vectorWeight, keywordWeight, RagConstant.RRF_CONSTANT_K,
                fusedResults.size(), formatContentBrief(fusedResults));

        return fusedResults;
    }

    /**
     * RRF（Reciprocal Rank Fusion）倒数排名融合算法
     * 公式：RRF_score(d) = w_vector / (K + rank_vector(d)) + w_keyword / (K + rank_keyword(d))
     * 同一文档在两路结果中都出现时，得分叠加；仅出现在一路时，另一路得分为 0
     * 天然实现去重：相同文本的文档只保留一份，分数合并
     *
     * @param vectorResults  向量检索结果（已按相似度排序）
     * @param keywordResults BM25 检索结果（已按 BM25 分数排序）
     * @param vectorWeight   向量检索权重
     * @param keywordWeight  关键词检索权重
     * @return 融合后按 RRF 分数降序排列的内容列表
     * @see <a href="https://zhuanlan.zhihu.com/p/1914270914654237406">RRF 算法原理</a>
     */
    private List<Content> rrfFuse(List<Content> vectorResults, List<Content> keywordResults,
                                   double vectorWeight, double keywordWeight) {
        int k = RagConstant.RRF_CONSTANT_K;

        // 使用文本内容作为去重键，LinkedHashMap 保持插入顺序
        Map<String, Content> contentMap = new LinkedHashMap<>();
        Map<String, Double> scoreMap = new HashMap<>();

        // 处理向量检索结果
        for (int i = 0; i < vectorResults.size(); i++) {
            String textKey = vectorResults.get(i).textSegment().text();
            contentMap.putIfAbsent(textKey, vectorResults.get(i));
            scoreMap.merge(textKey, vectorWeight / (k + i + 1), Double::sum);
        }

        // 处理关键词检索结果
        for (int i = 0; i < keywordResults.size(); i++) {
            String textKey = keywordResults.get(i).textSegment().text();
            contentMap.putIfAbsent(textKey, keywordResults.get(i));
            scoreMap.merge(textKey, keywordWeight / (k + i + 1), Double::sum);
        }

        // 按 RRF 分数降序排序
        return contentMap.entrySet().stream()
                .sorted((a, b) -> Double.compare(
                        scoreMap.getOrDefault(b.getKey(), 0.0),
                        scoreMap.getOrDefault(a.getKey(), 0.0)))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    /**
     * 格式化内容列表摘要（用于日志输出）
     * 每个片段取前 N 个字符，总长度超过限制时截断
     *
     * @param contents 内容列表
     * @return 格式化后的摘要字符串
     */
    private String formatContentBrief(List<Content> contents) {
        if (contents == null || contents.isEmpty()) {
            return "[]";
        }
        int briefLen = RagConstant.LOG_SEGMENT_BRIEF_LENGTH;
        StringJoiner joiner = new StringJoiner(", ");
        int totalLength = 0;
        for (Content content : contents) {
            String text = content.textSegment().text();
            String brief = text.length() > briefLen
                    ? text.substring(0, briefLen) + "..."
                    : text;
            brief = brief.replace("\n", " ").replace("\r", "");
            joiner.add("[" + brief + "]");
            totalLength += brief.length();
            if (totalLength > 200) {
                joiner.add("...(共" + contents.size() + "条)");
                break;
            }
        }
        return joiner.toString();
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
