package com.rich.codeweaver.model.dto.materialcategory;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 素材分类更新请求
 *
 * @author DuRuiChi
 * @since 2026-03-30
 */
@Data
public class MaterialCategoryUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 分类编码（唯一标识）
     */
    private String categoryCode;

    /**
     * 分类图标（Ant Design图标名）
     */
    private String categoryIcon;

    /**
     * 分类描述
     */
    private String description;

    /**
     * 排序权重（越小越靠前）
     */
    private Integer sortOrder;

    /**
     * 是否启用
     */
    private Integer isEnabled;
}
