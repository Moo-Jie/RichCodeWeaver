package com.rich.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 产物评论 VO（脱敏）
 * 包含评论内容、评论者信息、点赞状态等
 *
 * @author DuRuiChi
 * @since 2026-03-25
 */
@Data
public class AppCommentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评论id
     */
    private Long id;

    /**
     * 产物id
     */
    private Long appId;

    /**
     * 评论用户id
     */
    private Long userId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 评论点赞量
     */
    private Integer likeCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 评论用户信息
     */
    private UserVO user;

    /**
     * 当前用户是否已点赞该评论
     */
    private Boolean hasLiked;
}
