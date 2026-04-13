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
 * 产物热点统计 实体类
 * 存储产物的点赞量、转发量、收藏量、评论量等聚合数据
 *
 * @author DuRuiChi
 * @since 2026-03-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("app_hot_stat")
public class AppHotStat implements Serializable {

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
     * 点赞量
     */
    @Column("likeCount")
    private Integer likeCount;

    /**
     * 转发量
     */
    @Column("shareCount")
    private Integer shareCount;

    /**
     * 收藏量
     */
    @Column("favoriteCount")
    private Integer favoriteCount;

    /**
     * 评论量
     */
    @Column("commentCount")
    private Integer commentCount;

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
}
