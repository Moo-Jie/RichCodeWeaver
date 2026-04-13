package com.rich.codeweaver.common.constant;

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
     * 对话记忆窗口最大消息数
     */
    public static final int CHAT_MEMORY_MAX_MESSAGES = 50;

    /**
     * 工作流/Agent 历史加载条数
     */
    public static final int DEFAULT_HISTORY_LOAD_LIMIT = 10;

    /**
     * 客服历史加载条数
     */
    public static final int CUSTOMER_SERVICE_HISTORY_LOAD_LIMIT = 20;

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

    private AiServiceConstant() {
    }
}
