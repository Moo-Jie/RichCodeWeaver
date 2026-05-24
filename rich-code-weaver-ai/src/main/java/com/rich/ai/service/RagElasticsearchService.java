package com.rich.ai.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.rich.common.constant.RagConstant;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * RAG Elasticsearch BM25 关键词检索服务
 * 负责文档片段的 ES 索引管理与 BM25 关键词召回，
 * 配合向量检索实现混合检索（Hybrid Search）策略。
 * ES 不可用时自动降级为纯向量检索，不影响系统正常运行。
 *
 * @author DuRuiChi
 * @create 2026/5/19
 **/
@Slf4j
public class RagElasticsearchService {

    private static final String LOG_TAG = "RAG ES";

    /**
     * IK 分词器 - 索引时使用细粒度分词
     */
    private static final String INDEX_ANALYZER = "ik_max_word";

    /**
     * IK 分词器 - 智能中文分词模式
     */
    private static final String SEARCH_ANALYZER = "ik_smart";

    /**
     * ES 中 index 的 document 字段名，用于初始 index 结构的属性值映射
     */
    private static final String FIELD_TEXT = "text";
    private static final String FIELD_BIZ_TYPE = "bizType";
    private static final String FIELD_CODE_GEN_TYPE = "codeGenType";
    private static final String FIELD_SOURCE = "source";
    private static final String FIELD_TITLE = "title";

    private final ElasticsearchClient client;
    private final String indexName;

    /**
     * ES 可用状态标志，初始化失败或运行时异常时置为 false
     * -- GETTER --
     *  获取 ES 当前可用状态
     */
    @Getter
    private volatile boolean available;

    public RagElasticsearchService(ElasticsearchClient client, String indexName) {
        this.client = client;
        this.indexName = indexName;
        this.available = checkConnection();
    }

    /**
     * 初始化索引，确保索引存在，不存在则创建（初始化 index ）
     */
    public void InitializeIndex() {
        // 若 ES 服务异常，则跳过初始化
        if (!available) return;
        try {
            boolean exists = client.indices().exists(e -> e.index(indexName)).value();
            if (!exists) {
                // 创建索引
                client.indices().create(c -> c
                        .index(indexName)
                        // lambda 表达式嵌入属性注入逻辑
                        .mappings(m -> m
                                // document 的字段属性
                                .properties(FIELD_TEXT, p -> p.text(t -> t
                                        .analyzer(INDEX_ANALYZER) // 导入时的分词模式
                                        .searchAnalyzer(SEARCH_ANALYZER))) // 查询时的分词模式
                                .properties(FIELD_BIZ_TYPE, p -> p.keyword(k -> k))
                                .properties(FIELD_CODE_GEN_TYPE, p -> p.keyword(k -> k))
                                .properties(FIELD_SOURCE, p -> p.keyword(k -> k))
                                .properties(FIELD_TITLE, p -> p.keyword(k -> k))
                        )
                );
                log.info("【{}】创建 ES 索引 {} 完成（IK 分词器：索引={}, 搜索={}）",
                        LOG_TAG, indexName, INDEX_ANALYZER, SEARCH_ANALYZER);
            } else {
                log.info("【{}】ES 索引 {} 已存在，跳过创建", LOG_TAG, indexName);
            }
        } catch (Exception e) {
            log.error("【{}】创建 ES 索引失败，混合检索将降级为纯向量检索: {}", LOG_TAG, e.getMessage());
            available = false;
        }
    }

    /**
     * 批量索引文档片段到 ES
     * 使用文本内容的 MD5 作为文档 ID，保证幂等性
     *
     * @param segments 文档片段列表（含元数据）
     */
    public void indexSegments(List<TextSegment> segments) {
        // 前置检查：服务不可用、输入为空或列表为空时直接返回，避免无效操作
        if (!available || segments == null || segments.isEmpty()) return;

        try {
            // 创建 BulkRequest 请求，用于与 ES 进行批量操作
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();

            // 遍历每个文档片段，构建相应的 ES 索引操作，并加入批量请求对象中
            for (TextSegment segment : segments) {
                // 根据片段的文本内容生成 ID
                String docId = md5(segment.text());

                // 将 TextSegment 中的内容和元信息映射为 ES 文档的 Map 结构
                Map<String, Object> doc = buildEsDocument(segment);

                // 循环添加操作
                // op -> op.index(idx -> ...) 即为添加一个操作
                bulkBuilder.operations(op -> op.index(idx -> idx
                        .index(indexName) // 指定目标索引名称
                        .id(docId) // 指定文档 ID
                        .document(doc) // 设置文档内容
                ));
            }

            // 执行批量请求，获取响应结果
            BulkResponse response = client.bulk(bulkBuilder.build());

            // 判断批量操作中是否存在错误
            if (response.errors()) {
                // 统计出错的操作数量
                long errorCount = response.items().stream()
                        .filter(item -> item.error() != null)
                        .count();
                // 记录部分失败的警告日志，包含成功数和失败数
                log.warn("【{}】批量索引部分失败，成功: {}，失败: {}",
                        LOG_TAG, segments.size() - errorCount, errorCount);
            } else {
                // 全部成功，记录信息日志，输出文档数量及目标索引名
                log.info("【{}】批量索引完成，共 {} 个文档片段写入 ES 索引 {}",
                        LOG_TAG, segments.size(), indexName);
            }
        } catch (Exception e) {
            // 捕获所有异常，记录错误日志，并标记服务不可用
            // 这样设计是为了防止因 ES 临时故障导致后续请求持续抛出异常
            log.error("【{}】批量索引失败（不影响向量检索）: {}", LOG_TAG, e.getMessage());
            available = false;
        }
    }

    /**
     * 清空 ES 索引中的所有文档（用于 reindexAll）
     */
    public void deleteAll() {
        if (!available) return;
        try {
            boolean exists = client.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                // 删除指定名称的 index
                client.deleteByQuery(d -> d
                        .index(indexName)
                        .query(q -> q.matchAll(m -> m))
                );
                log.info("【{}】已清空 ES 索引 {} 中的所有文档", LOG_TAG, indexName);
            }
        } catch (Exception e) {
            log.error("【{}】清空 ES 索引失败: {}", LOG_TAG, e.getMessage());
        }
    }

    /**
     * BM25 关键词检索
     * 使用 IK 分词器对查询文本进行分词，然后基于 BM25 算法进行关键词匹配
     * 支持按 bizType 和 codeGenType 进行元数据过滤
     *
     * @param queryText    用户查询文本
     * @param bizType      业务类型过滤（不为空时生效）
     * @param codeGenTypes 代码生成类型过滤列表（不为空时生效）
     * @param maxResults   最大返回数量
     * @return 命中的内容列表，ES 不可用时返回空列表
     */
    @SuppressWarnings("rawtypes")
    public List<Content> searchBm25(String queryText, String bizType,
                                    List<String> codeGenTypes, int maxResults) {
        // 服务不可用时直接返回空列表，避免无效的远程调用
        if (!available) {
            return Collections.emptyList();
        }

        try {
            // 1. 构建 BM25 文本匹配查询
            Query bm25Query = Query.of(q -> q.match(m -> m
                    .field(FIELD_TEXT) // 检索字段名
                    .query(queryText) // 查询内容
                    .analyzer(SEARCH_ANALYZER) // 分词模式
            ));

            // 2. 构建元信息过滤条件
            List<Query> filters = buildFilters(bizType, codeGenTypes);

            // 3. 将文本查询与过滤条件组合为最终的 ES 查询
            Query finalQuery;
            if (filters.isEmpty()) {
                // 无过滤条件时直接使用 BM25 查询
                finalQuery = bm25Query;
            } else {
                // 有过滤条件时，使用 bool 查询：
                //   - must: 文本查询，影响相关性评分
                //   - filter: 过滤条件，不参与评分，可利用缓存提升性能
                finalQuery = Query.of(q -> q.bool(b -> {
                    b.must(bm25Query);
                    for (Query filter : filters) {
                        b.filter(filter);
                    }
                    return b;
                }));
            }

            // 4. 执行搜索请求
            SearchResponse<Map> response = client.search(s -> s
                            .index(indexName)      // 指定索引
                            .query(finalQuery)     // 设置组合查询
                            .size(maxResults),     // 限制返回条数
                    Map.class);            // 将原始 _source 映射为 Map

            // 5. 解析搜索结果，将每个 ES 命中记录转换为 Content 对象
            List<Content> results = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Content content = parseHitToContent(hit);
                if (content != null) {
                    results.add(content);
                }
            }

            return results;
        } catch (Exception e) {
            // 发生异常时记录警告日志，并将服务标记为不可用
            // 这样后续请求将直接返回空列表，避免对故障 ES 集群持续发起无效调用
            log.warn("【{}】BM25 检索失败，降级为纯向量检索: {}", LOG_TAG, e.getMessage());
            available = false;
            return Collections.emptyList();
        }
    }

    /**
     * 检测 ES 连接是否可用
     */
    private boolean checkConnection() {
        try {
            boolean pingOk = client.ping().value();
            if (pingOk) {
                log.info("【{}】Elasticsearch 连接成功，地址: {}", LOG_TAG, indexName);
            }
            return pingOk;
        } catch (Exception e) {
            log.warn("【{}】Elasticsearch 连接失败，混合检索将降级为纯向量检索: {}", LOG_TAG, e.getMessage());
            return false;
        }
    }

    /**
     * 构建 ES 文档（从 TextSegment 提取文本和元数据）
     */
    private Map<String, Object> buildEsDocument(TextSegment segment) {
        Map<String, Object> doc = new HashMap<>();
        doc.put(FIELD_TEXT, segment.text());
        Metadata meta = segment.metadata();
        doc.put(FIELD_BIZ_TYPE, meta.getString(RagConstant.METADATA_BIZ_TYPE));
        doc.put(FIELD_CODE_GEN_TYPE, meta.getString(RagConstant.METADATA_CODE_GEN_TYPE));
        doc.put(FIELD_SOURCE, meta.getString(RagConstant.METADATA_SOURCE));
        doc.put(FIELD_TITLE, meta.getString(RagConstant.METADATA_TITLE));
        return doc;
    }

    /**
     * 构建 ES 过滤条件列表
     */
    private List<Query> buildFilters(String bizType, List<String> codeGenTypes) {
        List<Query> filters = new ArrayList<>();
        if (bizType != null && !bizType.isBlank()) {
            filters.add(Query.of(q -> q.term(t -> t
                    .field(FIELD_BIZ_TYPE)
                    .value(bizType))));
        }
        if (codeGenTypes != null && !codeGenTypes.isEmpty()) {
            List<FieldValue> fieldValues = codeGenTypes.stream()
                    .map(FieldValue::of)
                    .toList();
            filters.add(Query.of(q -> q.terms(t -> t
                    .field(FIELD_CODE_GEN_TYPE)
                    .terms(tv -> tv.value(fieldValues)))));
        }
        return filters;
    }

    /**
     * 将 ES Hit 解析为 langchain4j Content 对象
     */
    @SuppressWarnings("unchecked")
    private Content parseHitToContent(Hit<Map> hit) {
        Map<String, Object> source = hit.source();
        if (source == null) return null;
        String text = (String) source.get(FIELD_TEXT);
        if (text == null || text.isBlank()) return null;

        Metadata metadata = new Metadata();
        putIfNotNull(metadata, RagConstant.METADATA_BIZ_TYPE, source.get(FIELD_BIZ_TYPE));
        putIfNotNull(metadata, RagConstant.METADATA_CODE_GEN_TYPE, source.get(FIELD_CODE_GEN_TYPE));
        putIfNotNull(metadata, RagConstant.METADATA_SOURCE, source.get(FIELD_SOURCE));
        putIfNotNull(metadata, RagConstant.METADATA_TITLE, source.get(FIELD_TITLE));

        return Content.from(TextSegment.from(text, metadata));
    }

    /**
     * 安全设置元数据值
     */
    private void putIfNotNull(Metadata metadata, String key, Object value) {
        if (value != null) {
            metadata.put(key, value.toString());
        }
    }

    /**
     * 计算文本的 MD5 摘要，作为 ES 文档的唯一 ID
     * 保证相同文本片段只会在 ES 中存储一份（幂等）
     */
    private String md5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(text.hashCode());
        }
    }
}
