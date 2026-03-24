package com.rich.model.dto.systemPrompt;

import lombok.Data;

import java.io.Serializable;

/**
 * 系统提示词新增请求（仅文件元数据，不含文件内容）
 *
 * @author DuRuiChi
 */
@Data
public class SystemPromptAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 提示词名称
     */
    private String promptName;

    /**
     * 提示词文件路径（相对于 resources，如 /aiPrompt/xxx.txt）
     */
    private String filePath;

    /**
     * 提示词描述
     */
    private String description;
}
