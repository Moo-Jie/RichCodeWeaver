package com.rich.richcodeweaver.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 图片类别枚举
 *
 * @author DuRuiChi
 * @create 2025/9/8
 **/
@Getter
public enum ImageCategoryEnum {

    CONTENT("内容图片", "CONTENT"),
    AI("AI 图片", "ai");


    private final String text;

    private final String value;

    ImageCategoryEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static ImageCategoryEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (ImageCategoryEnum anEnum : ImageCategoryEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
