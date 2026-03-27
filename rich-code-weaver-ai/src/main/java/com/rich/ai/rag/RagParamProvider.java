package com.rich.ai.rag;

/**
 * RAG 参数来源接口
 * 解耦参数来源（数据库 / 配置文件 / 默认值），由具体实现决定从哪里加载参数
 * 替代 RagDocumentIndexCreationService 和 RagContentRetrieverAugmentorFactory 中写死的常量
 *
 * @author DuRuiChi
 * @create 2026/3/27
 * @see RagDocumentIndexCreationService 消费此接口获取 ETL 参数
 * @see RagContentRetrieverAugmentorFactory 消费此接口获取检索参数
 */
public interface RagParamProvider {

    /**
     * 切片最大字符数（默认 500）
     * 对应 DB key: max_segment_size
     */
    int getMaxSegmentSize();

    /**
     * 切片重叠字符数（默认 50）
     * 对应 DB key: max_overlap_size
     */
    int getMaxOverlapSize();

    /**
     * 最大检索结果数 Top-K（默认 5）
     * 对应 DB key: max_results
     */
    int getMaxResults();

    /**
     * 最低相似度阈值（默认 0.6）
     * 对应 DB key: min_score
     */
    double getMinScore();

    /**
     * RAG 内容注入提示词模板
     * 对应 DB key: injection_prompt_template
     */
    String getInjectionPromptTemplate();
}
