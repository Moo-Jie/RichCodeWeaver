package com.rich.common.constant;

/**
 * RAG 相关常量
 *
 * @author DuRuiChi
 * @create 2026/4/9
 */
public final class RagConstant {

    /**
     * 默认通用代码生成类型
     */
    public static final String DEFAULT_CODE_GEN_TYPE = "GENERAL";

    /**
     * 默认文档标题
     */
    public static final String DEFAULT_DOCUMENT_TITLE = "未知文档";

    /**
     * 文档元数据 bizType 键
     */
    public static final String METADATA_BIZ_TYPE = "bizType";

    /**
     * 文档元数据 codeGenType 键
     */
    public static final String METADATA_CODE_GEN_TYPE = "codeGenType";

    /**
     * 文档元数据 source 键
     */
    public static final String METADATA_SOURCE = "source";

    /**
     * 文档元数据 title 键
     */
    public static final String METADATA_TITLE = "title";

    /**
     * 最大分段长度参数 key
     */
    public static final String PARAM_MAX_SEGMENT_SIZE = "max_segment_size";

    /**
     * 最大重叠长度参数 key
     */
    public static final String PARAM_MAX_OVERLAP_SIZE = "max_overlap_size";

    /**
     * 最大返回数量参数 key
     */
    public static final String PARAM_MAX_RESULTS = "max_results";

    /**
     * 最低分数参数 key
     */
    public static final String PARAM_MIN_SCORE = "min_score";

    /**
     * 注入模板参数 key
     */
    public static final String PARAM_INJECTION_PROMPT_TEMPLATE = "injection_prompt_template";

    /**
     * 整型参数类型
     */
    public static final String PARAM_TYPE_INT = "int";

    /**
     * 小数参数类型
     */
    public static final String PARAM_TYPE_DOUBLE = "double";

    /**
     * 文本区域参数类型
     */
    public static final String PARAM_TYPE_TEXTAREA = "textarea";

    /**
     * 用户消息占位符
     */
    public static final String TEMPLATE_USER_MESSAGE_PLACEHOLDER = "{{userMessage}}";

    /**
     * 检索内容占位符
     */
    public static final String TEMPLATE_CONTENTS_PLACEHOLDER = "{{contents}}";

    /**
     * RAG 未启用提示
     */
    public static final String RAG_DISABLED_MESSAGE = "RAG 功能未启用（rag.enabled=false），无法执行重新索引";

    /**
     * RAG 重建索引成功提示
     */
    public static final String RAG_REINDEX_SUCCESS_MESSAGE = "向量库重新索引完成，已同步最新知识库文档";

    /**
     * 默认最大分段长度
     */
    public static final int DEFAULT_MAX_SEGMENT_SIZE = 500;

    /**
     * 默认最大重叠长度
     */
    public static final int DEFAULT_MAX_OVERLAP_SIZE = 50;

    /**
     * 默认最大返回数量
     */
    public static final int DEFAULT_MAX_RESULTS = 5;

    /**
     * 默认最低分数
     */
    public static final double DEFAULT_MIN_SCORE = 0.6D;

    /**
     * 最低分数下界
     */
    public static final double MIN_SCORE_LOWER_BOUND = 0D;

    /**
     * 最低分数上界
     */
    public static final double MIN_SCORE_UPPER_BOUND = 1D;

    private RagConstant() {
    }
}
