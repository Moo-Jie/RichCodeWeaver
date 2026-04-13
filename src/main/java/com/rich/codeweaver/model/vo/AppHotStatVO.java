package com.rich.codeweaver.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 产物热点统计 VO（脱敏）
 * 对外提供产物的热点数据，解耦产物模块
 *
 * @author DuRuiChi
 * @since 2026-03-25
 */
@Data
public class AppHotStatVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 产物id
     */
    private Long appId;

    /**
     * 点赞量
     */
    private Integer likeCount;

    /**
     * 转发量
     */
    private Integer shareCount;

    /**
     * 收藏量
     */
    private Integer favoriteCount;

    /**
     * 评论量
     */
    private Integer commentCount;

    /**
     * 当前用户是否已点赞
     */
    private Boolean hasLiked;

    /**
     * 当前用户是否已收藏
     */
    private Boolean hasFavorited;
}
