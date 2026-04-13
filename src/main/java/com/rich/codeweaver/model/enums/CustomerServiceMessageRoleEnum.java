package com.rich.codeweaver.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * AI 客服消息发送方枚举
 *
 * @author DuRuiChi
 * @create 2026/4/9
 */
@Getter
public enum CustomerServiceMessageRoleEnum {

    USER("用户", "user"),
    AI("AI", "ai");

    private final String text;

    private final String value;

    CustomerServiceMessageRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static CustomerServiceMessageRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (CustomerServiceMessageRoleEnum anEnum : CustomerServiceMessageRoleEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
