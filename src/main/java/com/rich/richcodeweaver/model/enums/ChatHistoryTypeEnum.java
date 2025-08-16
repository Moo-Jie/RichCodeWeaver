package com.rich.richcodeweaver.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 对话历史消息枚举类型
 *
 * @author DuRuiChi
 * @create 2025/8/16
 **/
@Getter
public enum ChatHistoryTypeEnum {

    /**
     * 对话历史消息枚举类型，区分用户和 AI 的对话
     */
    USER("用户", "user"),
    AI("AI", "ai");

    private final String text;

    private final String value;

    ChatHistoryTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return com.rich.richcodeweaver.model.enums.ChatHistoryTypeEnum 枚举值
     * @author DuRuiChi
     * @create 2025/8/16
     **/
    public static ChatHistoryTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (ChatHistoryTypeEnum anEnum : ChatHistoryTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}