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
 * 素材 实体类
 *
 * @author DuRuiChi
 * @since 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("material")
public class Material implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id（雪花）
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 素材名称
     */
    @Column("materialName")
    private String materialName;

    /**
     * 分类id
     */
    @Column("categoryId")
    private Long categoryId;

    /**
     * 素材类型：image/text/video/audio/link
     */
    @Column("materialType")
    private String materialType;

    /**
     * 素材内容（URL或文本内容）
     */
    private String content;

    /**
     * 缩略图URL（图片/视频类素材）
     */
    @Column("thumbnailUrl")
    private String thumbnailUrl;

    /**
     * 文件大小（字节，URL类素材可为空）
     */
    @Column("fileSize")
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
    @Column("isPublic")
    private Integer isPublic;

    /**
     * 使用次数
     */
    @Column("useCount")
    private Integer useCount;

    /**
     * 所属用户id
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
