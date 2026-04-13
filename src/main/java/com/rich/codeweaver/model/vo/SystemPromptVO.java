package com.rich.codeweaver.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统提示词 视图对象
 *
 * @author DuRuiChi
 */
@Data
public class SystemPromptVO implements Serializable {

    @Serial
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
     * 提示词唯一标识
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

    /**
     * 创建人id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
