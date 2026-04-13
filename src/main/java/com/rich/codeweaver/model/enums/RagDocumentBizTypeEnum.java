package com.rich.codeweaver.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * RAG 文档业务类型枚举
 * 用于隔离代码生成知识库与 AI 客服知识库
 *
 * @author DuRuiChi
 * @create 2026/4/9
 */
@Getter
public enum RagDocumentBizTypeEnum {

    CODE_GEN("代码生成", "CODE_GEN"),
    CUSTOMER_SERVICE("AI 客服", "CUSTOMER_SERVICE");

    private final String text;

    private final String value;

    RagDocumentBizTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static RagDocumentBizTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (RagDocumentBizTypeEnum anEnum : RagDocumentBizTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
