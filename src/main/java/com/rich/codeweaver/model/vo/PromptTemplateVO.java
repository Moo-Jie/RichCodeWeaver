package com.rich.codeweaver.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 提示词模板 VO（脱敏）
 */
@Data
public class PromptTemplateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 匹配身份
     */
    private String matchIdentity;

    /**
     * 匹配行业领域
     */
    private String matchIndustry;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 提示词内容（含占位符）
     */
    private String promptContent;

    /**
     * 模板可编辑字段定义（JSON数组）
     */
    private String templateFields;

    /**
     * 排序权重
     */
    private Integer sortOrder;

    /**
     * 是否启用
     */
    private Integer isEnabled;

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
