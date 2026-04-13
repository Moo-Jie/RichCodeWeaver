package com.rich.codeweaver.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 提示词模板 实体类
 *
 * @author DuRuiChi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("prompt_template")
public class PromptTemplate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id（雪花）
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 模板名称
     */
    @Column("templateName")
    private String templateName;

    /**
     * 匹配身份（individual/merchant/enterprise），NULL表示不限
     */
    @Column("matchIdentity")
    private String matchIdentity;

    /**
     * 匹配行业领域，NULL表示不限
     */
    @Column("matchIndustry")
    private String matchIndustry;

    /**
     * 模板描述
     */
    @Column("description")
    private String description;

    /**
     * 提示词内容（含占位符 {{key}}）
     */
    @Column("promptContent")
    private String promptContent;

    /**
     * 模板可编辑字段定义（JSON数组）
     */
    @Column("templateFields")
    private String templateFields;

    /**
     * 排序权重（越小越靠前）
     */
    @Column("sortOrder")
    private Integer sortOrder;

    /**
     * 是否启用
     */
    @Column("isEnabled")
    private Integer isEnabled;

    /**
     * 创建人id
     */
    @Column("userId")
    private Long userId;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column("updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
