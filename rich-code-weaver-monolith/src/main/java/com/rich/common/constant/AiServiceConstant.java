package com.rich.common.constant;

/**
 * AI 服务相关常量
 *
 * @author DuRuiChi
 * @create 2026/4/9
 */
public final class AiServiceConstant {

    /**
     * HTML 工作流系统提示词 key
     */
    public static final String HTML_SYSTEM_PROMPT_KEY = "html-system-prompt";

    /**
     * 多文件工作流系统提示词 key
     */
    public static final String MULTI_FILE_SYSTEM_PROMPT_KEY = "multi-file-system-prompt";

    /**
     * Vue 项目工作流系统提示词 key
     */
    public static final String VUE_PROJECT_SYSTEM_PROMPT_KEY = "vue-project-system-prompt";

    /**
     * Agent 系统提示词 key
     */
    public static final String AGENT_SYSTEM_PROMPT_KEY = "agent-code-gen-system-prompt";

    /**
     * 普通流式模型 bean 名称
     */
    public static final String STREAMING_CHAT_MODEL_BEAN = "streamingChatModel";

    /**
     * 推理流式模型 bean 名称
     */
    public static final String REASONING_STREAMING_CHAT_MODEL_BEAN = "reasoningStreamingChatModel";

    /**
     * 客服流式模型 bean 名称
     */
    public static final String CUSTOMER_SERVICE_STREAMING_CHAT_MODEL_BEAN = "customerServiceStreamingChatModel";

    /**
     * 历史记录加载失败提示
     */
    public static final String HISTORY_LOAD_FAILED_MESSAGE = "历史记录加载失败，可能已经过期。";

    /**
     * 工具幻觉错误文案模板
     */
    public static final String HALLUCINATED_TOOL_MESSAGE_TEMPLATE = "Error: there is no tool called %s";

    /**
     * 对话记忆窗口最大消息数（兜底，仅在 Tokenizer 不可用时降级使用）
     */
    public static final int CHAT_MEMORY_MAX_MESSAGES = 20;

    /**
     * 对话记忆 Token 滑动窗口最大 Token 数
     * 窗口根据实际 Token 消耗动态淘汰最旧消息，比固定条数窗口更精确
     */
    public static final int CHAT_MEMORY_MAX_TOKENS = 128000;

    /**
     * 工具消息折叠：保留最近 N 组工具调用不折叠
     * 超出部分的 ToolExecutionResultMessage 正文和 AiMessage 工具参数会被压缩
     */
    public static final int CHAT_MEMORY_TOOL_FOLD_KEEP_RECENT = 4;

    /**
     * 工具执行结果折叠后的替代正文
     */
    public static final String CHAT_MEMORY_TOOL_FOLDED_RESULT = "[工具执行结果已折叠以节省上下文]";

    /**
     * 工具调用参数折叠后的替代 JSON
     */
    public static final String CHAT_MEMORY_TOOL_FOLDED_ARGUMENTS = "{\"_summary\":\"参数已折叠以节省上下文\"}";

    /**
     * 工作流/Agent 历史加载条数
     */
    public static final int DEFAULT_HISTORY_LOAD_LIMIT = 50;

    /**
     * 客服历史加载条数
     */
    public static final int CUSTOMER_SERVICE_HISTORY_LOAD_LIMIT = 100;

    /**
     * AI 服务缓存最大数量
     */
    public static final int AI_SERVICE_CACHE_MAX_SIZE = 1000;

    /**
     * AI 服务写入过期分钟数
     */
    public static final long AI_SERVICE_EXPIRE_AFTER_WRITE_MINUTES = 30L;

    /**
     * AI 服务访问过期分钟数
     */
    public static final long AI_SERVICE_EXPIRE_AFTER_ACCESS_MINUTES = 10L;

    /**
     * 工作流模式最大工具调用次数
     */
    public static final int WORKFLOW_MAX_TOOLS_INVOCATIONS = 25;

    /**
     * Agent 模式最大工具调用次数
     */
    public static final int AGENT_MAX_TOOLS_INVOCATIONS = 40;

    /**
     * Token 滑动窗口 Tokenizer 默认模型名称（用于近似 Token 计数）
     */
    public static final String CHAT_MEMORY_TOKENIZER_MODEL = "gpt-4o";

    /**
     * 对话记忆 Redis 分片大小（字符数）
     */
    public static final int CHAT_MEMORY_CHUNK_SIZE = 4096;

    /**
     * 对话记忆单条消息最多允许拆分的 Redis 分片数
     */
    public static final int CHAT_MEMORY_MAX_CHUNKS_PER_MESSAGE = 32;

    /**
     * 对话记忆超大消息兜底截断长度
     * 当单条消息拆分后仍然过大时，再对正文做截断
     */
    public static final int CHAT_MEMORY_MAX_MESSAGE_CHARS = 131072;

    /**
     * 消息截断后缀标记，提示 AI 内容已被裁剪
     */
    public static final String CHAT_MEMORY_TRUNCATION_SUFFIX = "\n...[内容过长，已截断以节省上下文空间]";

    // ======================== 近期增量摘要记忆 ========================

    /**
     * 增量摘要触发轮数阈值：累计新增满 N 轮对话触发一次 LLM 摘要生成
     */
    public static final int SUMMARY_TRIGGER_ROUND_COUNT = 5;

    /**
     * 摘要生成系统提示词 key（对应 system_prompt 表的 promptKey）
     */
    public static final String SUMMARY_SYSTEM_PROMPT_KEY = "ai-chat-summary-system-prompt";

    /**
     * 摘要生成用 ChatModel Bean 名称
     */
    public static final String SUMMARY_CHAT_MODEL_BEAN = "summaryChatModel";

    /**
     * 摘要类型枚举值
     */
    public static final String SUMMARY_TYPE_AGENT = "AGENT";
    public static final String SUMMARY_TYPE_WORKFLOW = "WORKFLOW";
    public static final String SUMMARY_TYPE_CUSTOMER_SERVICE = "CUSTOMER_SERVICE";

    /**
     * 摘要注入到上下文时的前缀标记（拼接在 SystemMessage 末尾或作为首条 UserMessage 前缀）
     */
    public static final String SUMMARY_CONTEXT_PREFIX = "\n\n[近期对话摘要]\n";

    private AiServiceConstant() {
    }
}
