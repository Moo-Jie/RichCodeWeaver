package com.rich.richcodeweaver.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 代码生成器的不同模式
 *
 * @author DuRuiChi
 * @create 2025/8/5
 **/
@Getter
public enum CodeGeneratorTypeEnum {

    HTML("单 HTML 页面模式", "single_html"),
    MULTI_FILE("多文件结构模式", "multi_file"),
    VUE_PROJECT("Vue 项目工程模式", "vue_project");
    // TODO 其他模式

    private final String text;

    private final String value;

    CodeGeneratorTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的 value
     * @return 枚举值
     */
    public static CodeGeneratorTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (CodeGeneratorTypeEnum anEnum : CodeGeneratorTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
