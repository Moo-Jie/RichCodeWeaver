package com.rich.social.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.model.entity.CommunityPost;
import com.rich.model.entity.CommunityPostLike;
import com.rich.social.mapper.CommunityPostLikeMapper;
import com.rich.social.service.CommunityPostLikeService;
import com.rich.social.service.CommunityPostService;
import com.rich.social.utils.SocialRedisHelper;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 社区帖子点赞服务实现。
 * 通过 Redis 分布式锁保证点赞 / 取消点赞在并发场景下的安全性。
 */
@Service
public class CommunityPostLikeServiceImpl extends ServiceImpl<CommunityPostLikeMapper, CommunityPostLike>
        implements CommunityPostLikeService {

    @Resource
    @Lazy
    private CommunityPostService communityPostService;

    @Resource
    private SocialRedisHelper socialRedisHelper;

    /**
     * 切换帖子点赞状态。
     * 若已存在点赞记录则删除，否则新增点赞记录，并同步更新帖子点赞数。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean togglePostLike(Long postId, Long userId) {
        CommunityPost post = communityPostService.getById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        }
        String lockKey = SocialRedisHelper.communityPostLikeLockKey(postId, userId);
        boolean locked = socialRedisHelper.tryLock(lockKey);
        if (!locked) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作频繁，请稍后重试");
        }
        try {
            QueryWrapper query = QueryWrapper.create()
                    .from(CommunityPostLike.class)
                    .where("postId = ? AND userId = ?", postId, userId);
            CommunityPostLike existing = getOne(query);
            if (existing != null) {
                removeById(existing.getId());
                UpdateChain.of(CommunityPost.class)
                        .setRaw("likeCount", "GREATEST(likeCount - 1, 0)")
                        .where("id = ?", postId)
                        .update();
                return false;
            }
            CommunityPostLike postLike = CommunityPostLike.builder()
                    .postId(postId)
                    .userId(userId)
                    .build();
            save(postLike);
            UpdateChain.of(CommunityPost.class)
                    .setRaw("likeCount", "likeCount + 1")
                    .where("id = ?", postId)
                    .update();
            return true;
        } finally {
            socialRedisHelper.releaseLock(lockKey);
        }
    }

    /**
     * 判断用户是否已经点赞当前帖子。
     */
    @Override
    public boolean hasLiked(Long postId, Long userId) {
        if (userId == null) {
            return false;
        }
        QueryWrapper query = QueryWrapper.create()
                .from(CommunityPostLike.class)
                .where("postId = ? AND userId = ?", postId, userId);
        return count(query) > 0;
    }
}
