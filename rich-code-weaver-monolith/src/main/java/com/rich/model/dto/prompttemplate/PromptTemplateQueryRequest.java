package com.rich.model.dto.prompttemplate;

import com.rich.common.model.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 提示词模板查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PromptTemplateQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 模板名称（模糊搜索）
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
     * 是否启用
     */
    private Integer isEnabled;
}
