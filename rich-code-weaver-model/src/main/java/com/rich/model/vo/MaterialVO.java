package com.rich.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 素材 视图对象
 *
 * @author DuRuiChi
 * @since 2026-03-30
 */
@Data
public class MaterialVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 素材名称
     */
    private String materialName;

    /**
     * 分类id
     */
    private Long categoryId;

    /**
     * 分类名称（关联查询）
     */
    private String categoryName;

    /**
     * 素材类型：image/text/video/audio/link
     */
    private String materialType;

    /**
     * 素材内容（URL或文本内容）
     */
    private String content;

    /**
     * 缩略图URL
     */
    private String thumbnailUrl;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 素材描述/备注
     */
    private String description;

    /**
     * 标签（逗号分隔）
     */
    private String tags;

    /**
     * 是否公开
     */
    private Integer isPublic;

    /**
     * 使用次数
     */
    private Integer useCount;

    /**
     * 所属用户id
     */
    private Long userId;

    /**
     * 所属用户信息
     */
    private UserVO user;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
