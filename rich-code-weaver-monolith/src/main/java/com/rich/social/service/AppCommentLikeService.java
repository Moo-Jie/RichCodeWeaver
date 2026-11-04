package com.rich.social.service;

import com.mybatisflex.core.service.IService;
import com.rich.model.entity.AppCommentLike;

/**
 * 评论点赞 服务层
 * 提供评论点赞/取消点赞的业务逻辑
 *
 * @author DuRuiChi
 * @since 2026-03-25
 */
public interface AppCommentLikeService extends IService<AppCommentLike> {

    /**
     * 切换评论点赞状态（已点赞则取消，未点赞则新增）
     *
     * @param commentId 评论id
     * @param userId    用户id
     * @return true=点赞成功, false=取消点赞成功
     */
    boolean toggleCommentLike(Long commentId, Long userId);

    /**
     * 检查用户是否已点赞某评论
     *
     * @param commentId 评论id
     * @param userId    用户id
     * @return 是否已点赞
     */
    boolean hasLiked(Long commentId, Long userId);
}
