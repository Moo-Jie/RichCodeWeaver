package com.rich.codeweaver.service.social;

import com.mybatisflex.core.service.IService;
import com.rich.codeweaver.model.entity.CommunityPostLike;

/**
 * 社区帖子点赞服务接口。
 */
public interface CommunityPostLikeService extends IService<CommunityPostLike> {

    /**
     * 切换帖子点赞状态。
     */
    boolean togglePostLike(Long postId, Long userId);

    /**
     * 判断当前用户是否已点赞帖子。
     */
    boolean hasLiked(Long postId, Long userId);
}
