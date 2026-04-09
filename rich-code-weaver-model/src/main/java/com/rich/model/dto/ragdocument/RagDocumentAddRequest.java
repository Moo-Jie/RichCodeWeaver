package com.rich.model.dto.ragdocument;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * RAG 知识库文档 新增请求
 *
 * @author DuRuiChi
 * @create 2026/3/27
 */
@Data
public class RagDocumentAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文档业务类型：CODE_GEN / CUSTOMER_SERVICE
     */
    private String bizType;

    /**
     * 文档标题
     */
    private String docTitle;

    /**
     * 文档正文内容（Markdown 格式）
     */
    private String docContent;

    /**
     * 适用的代码生成类型：HTML / MULTI_FILE / VUE_PROJECT / GENERAL
     */
    private String codeGenType;

    /**
     * 文档描述
     */
    private String description;

    /**
     * 是否启用（1-启用，0-禁用）
     */
    private Integer isEnabled;

    /**
     * 排序权重
     */
    private Integer sortOrder;
}
