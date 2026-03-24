package com.rich.model.dto.systemPrompt;

import lombok.Data;

import java.io.Serializable;

/**
 * 系统提示词更新请求（仅文件元数据，不含文件内容）
 *
 * @author DuRuiChi
 */
@Data
public class SystemPromptUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 提示词名称
     */
    private String promptName;

    /**
     * 提示词文件路径
     */
    private String filePath;

    /**
     * 提示词描述
     */
    private String description;
}
