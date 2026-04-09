package com.rich.common.constant;

/**
 * 流式事件常量
 *
 * @author DuRuiChi
 * @create 2026/4/9
 */
public final class StreamEventConstant {

    /**
     * 结束事件
     */
    public static final String EVENT_END = "end";

    /**
     * 普通消息事件
     */
    public static final String EVENT_MESSAGE = "message";

    /**
     * 服务端错误事件
     */
    public static final String EVENT_SERVER_ERROR = "server_error";

    /**
     * 通用错误事件
     */
    public static final String EVENT_ERROR = "error";

    /**
     * 数据块字段名
     */
    public static final String DATA_BLOCK_KEY = "b";

    /**
     * 错误字段名
     */
    public static final String DATA_ERROR_KEY = "error";

    /**
     * 空数据
     */
    public static final String EMPTY_DATA = "";

    private StreamEventConstant() {
    }
}
