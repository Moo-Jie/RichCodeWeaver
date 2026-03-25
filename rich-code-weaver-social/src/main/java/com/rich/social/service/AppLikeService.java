package com.rich.social.service;

import com.mybatisflex.core.service.IService;
import com.rich.model.entity.AppLike;

/**
 * 产物点赞 服务层
 * 提供产物点赞/取消点赞的业务逻辑
 *
 * @author DuRuiChi
 * @since 2026-03-25
 */
public interface AppLikeService extends IService<AppLike> {

    /**
     * 切换点赞状态（已点赞则取消，未点赞则新增）
     *
     * @param appId  产物id
     * @param userId 用户id
     * @return true=点赞成功, false=取消点赞成功
     */
    boolean toggleLike(Long appId, Long userId);

    /**
     * 检查用户是否已点赞
     *
     * @param appId  产物id
     * @param userId 用户id
     * @return 是否已点赞
     */
    boolean hasLiked(Long appId, Long userId);
}
