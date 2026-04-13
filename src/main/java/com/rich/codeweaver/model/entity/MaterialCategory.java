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
 * 素材分类 实体类
 *
 * @author DuRuiChi
 * @since 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("material_category")
public class MaterialCategory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id（雪花）
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 分类名称
     */
    @Column("categoryName")
    private String categoryName;

    /**
     * 分类编码（唯一标识）
     */
    @Column("categoryCode")
    private String categoryCode;

    /**
     * 分类图标（Ant Design图标名）
     */
    @Column("categoryIcon")
    private String categoryIcon;

    /**
     * 分类描述
     */
    private String description;

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
     * 创建人id（NULL表示系统预置）
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
