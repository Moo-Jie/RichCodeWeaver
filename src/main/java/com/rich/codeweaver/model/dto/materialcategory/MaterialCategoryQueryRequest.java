package com.rich.codeweaver.model.dto.materialcategory;

import com.rich.codeweaver.common.model.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 素材分类查询请求
 *
 * @author DuRuiChi
 * @since 2026-03-30
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MaterialCategoryQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 分类名称（模糊搜索）
     */
    private String categoryName;

    /**
     * 分类编码
     */
    private String categoryCode;

    /**
     * 是否启用
     */
    private Integer isEnabled;
}
