package com.rich.codeweaver.service.social;

import com.mybatisflex.core.service.IService;
import com.rich.codeweaver.model.entity.CommunityReplyLike;

/**
 * 社区回复点赞服务接口。
 */
public interface CommunityReplyLikeService extends IService<CommunityReplyLike> {

    /**
     * 切换回复点赞状态。
     */
    boolean toggleReplyLike(Long replyId, Long userId);

    /**
     * 判断当前用户是否已点赞回复。
     */
    boolean hasLiked(Long replyId, Long userId);
}
