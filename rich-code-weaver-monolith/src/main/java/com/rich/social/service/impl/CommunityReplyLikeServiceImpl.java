package com.rich.social.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.model.entity.CommunityReply;
import com.rich.model.entity.CommunityReplyLike;
import com.rich.social.mapper.CommunityReplyLikeMapper;
import com.rich.social.service.CommunityReplyLikeService;
import com.rich.social.service.CommunityReplyService;
import com.rich.social.utils.SocialRedisHelper;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 社区回复点赞服务实现。
 * 通过 Redis 分布式锁确保回复点赞切换的并发安全。
 */
@Service
public class CommunityReplyLikeServiceImpl extends ServiceImpl<CommunityReplyLikeMapper, CommunityReplyLike>
        implements CommunityReplyLikeService {

    @Resource
    @Lazy
    private CommunityReplyService communityReplyService;

    @Resource
    private SocialRedisHelper socialRedisHelper;

    /**
     * 切换回复点赞状态。
     * 若用户已点赞则取消，否则新增点赞，并同步更新回复点赞数。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleReplyLike(Long replyId, Long userId) {
        CommunityReply reply = communityReplyService.getById(replyId);
        if (reply == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "回复不存在");
        }
        String lockKey = SocialRedisHelper.communityReplyLikeLockKey(replyId, userId);
        boolean locked = socialRedisHelper.tryLock(lockKey);
        if (!locked) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作频繁，请稍后重试");
        }
        try {
            QueryWrapper query = QueryWrapper.create()
                    .from(CommunityReplyLike.class)
                    .where("replyId = ? AND userId = ?", replyId, userId);
            CommunityReplyLike existing = getOne(query);
            if (existing != null) {
                removeById(existing.getId());
                UpdateChain.of(CommunityReply.class)
                        .setRaw("likeCount", "GREATEST(likeCount - 1, 0)")
                        .where("id = ?", replyId)
                        .update();
                return false;
            }
            CommunityReplyLike replyLike = CommunityReplyLike.builder()
                    .replyId(replyId)
                    .userId(userId)
                    .build();
            save(replyLike);
            UpdateChain.of(CommunityReply.class)
                    .setRaw("likeCount", "likeCount + 1")
                    .where("id = ?", replyId)
                    .update();
            return true;
        } finally {
            socialRedisHelper.releaseLock(lockKey);
        }
    }

    /**
     * 判断用户是否已经点赞当前回复。
     */
    @Override
    public boolean hasLiked(Long replyId, Long userId) {
        if (userId == null) {
            return false;
        }
        QueryWrapper query = QueryWrapper.create()
                .from(CommunityReplyLike.class)
                .where("replyId = ? AND userId = ?", replyId, userId);
        return count(query) > 0;
    }
}
