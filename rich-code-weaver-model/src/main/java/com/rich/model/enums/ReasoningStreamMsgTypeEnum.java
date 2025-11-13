package com.rich.model.enums;

import lombok.Getter;

/**
 * 推理模型流式输出的枚举类型
 *
 * @author DuRuiChi
 * @create 2025/8/25
 **/
@Getter
public enum ReasoningStreamMsgTypeEnum {
    AI_RESPONSE("ai_response", "AI 响应信息"),
    TOOL_REQUEST("tool_request", "工具请求信息"),
    TOOL_EXECUTED("tool_executed", "工具执行结果信息");
    // TODO 其他模式

    private final String value;
    private final String text;

    ReasoningStreamMsgTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值
     * @return com.rich.richcodeweaver.model.enums.ReasoningStreamMsgTypeEnum 枚举
     * @author DuRuiChi
     * @create 2025/8/25
     **/
    public static ReasoningStreamMsgTypeEnum getEnumByValue(String value) {
        for (ReasoningStreamMsgTypeEnum typeEnum : values()) {
            if (typeEnum.getValue().equals(value)) {
                return typeEnum;
            }
        }
        return null;
    }
}
