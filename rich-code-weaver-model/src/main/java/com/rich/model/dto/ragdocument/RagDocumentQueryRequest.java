package com.rich.model.dto.ragdocument;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * RAG 知识库文档 分页查询请求
 *
 * @author DuRuiChi
 * @create 2026/3/27
 */
@Data
public class RagDocumentQueryRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    /**
     * 文档标题（模糊查询）
     */
    private String docTitle;

    /**
     * 适用的代码生成类型：HTML / MULTI_FILE / VUE_PROJECT / GENERAL
     */
    private String codeGenType;

    /**
     * 是否启用（1-启用，0-禁用）
     */
    private Integer isEnabled;
}
