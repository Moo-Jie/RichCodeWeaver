package com.rich.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * RAG 知识库文档 VO
 *
 * @author DuRuiChi
 * @create 2026/3/27
 */
@Data
public class RagDocumentVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 文档业务类型：CODE_GEN / CUSTOMER_SERVICE
     */
    private String bizType;

    private String docTitle;

    private String docContent;

    /**
     * 适用的代码生成类型：HTML / MULTI_FILE / VUE_PROJECT / GENERAL
     */
    private String codeGenType;

    private String description;

    /**
     * 是否启用（1-启用，0-禁用）
     */
    private Integer isEnabled;

    private Integer sortOrder;

    private Long userId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
