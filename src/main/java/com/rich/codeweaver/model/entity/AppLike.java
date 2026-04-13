package com.rich.codeweaver.model.entity;

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
 * 产物点赞记录 实体类
 * 记录用户对产物的点赞行为，联合唯一约束防止重复点赞
 *
 * @author DuRuiChi
 * @since 2026-03-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("app_like")
public class AppLike implements Serializable {

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
     * 用户id
     */
    @Column("userId")
    private Long userId;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;
}
