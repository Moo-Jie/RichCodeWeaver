package com.rich.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 素材分类 视图对象
 *
 * @author DuRuiChi
 * @since 2026-03-30
 */
@Data
public class MaterialCategoryVO implements Serializable {

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
     * 分类编码
     */
    private String categoryCode;

    /**
     * 分类图标
     */
    private String categoryIcon;

    /**
     * 分类描述
     */
    private String description;

    /**
     * 排序权重
     */
    private Integer sortOrder;

    /**
     * 是否启用
     */
    private Integer isEnabled;

    /**
     * 该分类下的素材数量
     */
    private Integer materialCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
