package com.rich.common.constant;

/**
 * AI 客服相关常量
 *
 * @author DuRuiChi
 * @create 2026/4/9
 */
public final class CustomerServiceConstant {

    /**
     * AI 客服系统提示词 key
     */
    public static final String CUSTOMER_SERVICE_PROMPT_KEY = "customer-service-system-prompt";

    /**
     * 游客会话 key
     */
    public static final String GUEST_VISITOR_SESSION_KEY = "customer_service_guest_key";

    /**
     * 用户身份前缀
     */
    public static final String USER_IDENTITY_PREFIX = "user:";

    /**
     * 游客身份前缀
     */
    public static final String GUEST_IDENTITY_PREFIX = "guest:";

    /**
     * AI 客服流式会话 key 模板
     */
    public static final String SESSION_KEY_TEMPLATE = "customer-service-session-%d-%d";

    /**
     * 默认会话标题
     */
    public static final String DEFAULT_CONVERSATION_TITLE = "新对话";

    /**
     * 单次最大消息分页大小
     */
    public static final int MAX_MESSAGE_PAGE_SIZE = 100;

    /**
     * 会话摘要最大长度
     */
    public static final int MAX_PREVIEW_LENGTH = 120;

    /**
     * 会话标题最大长度
     */
    public static final int MAX_TITLE_LENGTH = 24;

    private CustomerServiceConstant() {
    }
}
