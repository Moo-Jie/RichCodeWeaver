package com.rich.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 产物评论 实体类
 * 存储用户对产物的评论内容及评论的点赞量
 *
 * @author DuRuiChi
 * @since 2026-03-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("app_comment")
public class AppComment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 产物id
     */
    @Column("appId")
    private Long appId;

    /**
     * 评论用户id
     */
    @Column("userId")
    private Long userId;

    /**
     * 评论内容
     */
    @Column("content")
    private String content;

    /**
     * 评论点赞量
     */
    @Column("likeCount")
    private Integer likeCount;

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
     * 是否删除（逻辑删除）
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
