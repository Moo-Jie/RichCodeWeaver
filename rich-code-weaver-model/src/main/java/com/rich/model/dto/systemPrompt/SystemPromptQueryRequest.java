package com.rich.model.dto.systemPrompt;

import lombok.Data;

import java.io.Serializable;

/**
 * 系统提示词查询请求
 *
 * @author DuRuiChi
 */
@Data
public class SystemPromptQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 页码
     */
    private long pageNum = 1;

    /**
     * 页大小
     */
    private long pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序方式
     */
    private String sortOrder;

    /**
     * id
     */
    private Long id;

    /**
     * 提示词名称
     */
    private String promptName;

    /**
     * 文件路径
     */
    private String filePath;
}
