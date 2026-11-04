package com.rich.model.dto.material;

import com.rich.common.model.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 素材查询请求
 *
 * @author DuRuiChi
 * @since 2026-03-30
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MaterialQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 素材名称（模糊搜索）
     */
    private String materialName;

    /**
     * 分类id
     */
    private Long categoryId;

    /**
     * 素材类型：image/text/video/audio/link
     */
    private String materialType;

    /**
     * 标签（模糊搜索）
     */
    private String tags;

    /**
     * 是否公开（0-私有，1-公开）
     */
    private Integer isPublic;

    /**
     * 所属用户id
     */
    private Long userId;

    /**
     * 搜索关键词（搜索名称、描述、标签）
     */
    private String searchText;
}
