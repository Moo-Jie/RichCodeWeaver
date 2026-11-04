package com.rich.model.dto.material;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 素材更新请求
 *
 * @author DuRuiChi
 * @since 2026-03-30
 */
@Data
public class MaterialUpdateRequest implements Serializable {

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
     * 素材类型：image/text/video/audio/link
     */
    private String materialType;

    /**
     * 素材内容（URL或文本内容）
     */
    private String content;

    /**
     * 缩略图URL（图片/视频类素材）
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
     * 是否公开（0-私有，1-公开）
     */
    private Integer isPublic;
}
