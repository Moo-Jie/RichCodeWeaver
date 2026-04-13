package com.rich.codeweaver.model.dto.systemPrompt;

import lombok.Data;

import java.io.Serializable;

/**
 * 系统提示词新增请求
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
     * 提示词唯一标识（如 html-system-prompt）
     */
    private String promptKey;

    /**
     * 提示词内容
     */
    private String promptContent;

    /**
     * 提示词描述
     */
    private String description;
}
