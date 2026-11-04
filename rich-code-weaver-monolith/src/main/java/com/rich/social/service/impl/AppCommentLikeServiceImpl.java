package com.rich.social.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.model.entity.AppComment;
import com.rich.model.entity.AppCommentLike;
import com.rich.social.mapper.AppCommentLikeMapper;
import com.rich.social.service.AppCommentLikeService;
import com.rich.social.service.AppCommentService;
import com.rich.social.utils.SocialRedisHelper;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 评论点赞 服务实现
 * 实现评论点赞/取消点赞的业务逻辑，同步更新评论的likeCount
 * 使用 Redis 分布式锁防止同一用户并发操作导致的数据不一致
 *
 * @author DuRuiChi
 * @create 2026-03-25
 */
@Service
public class AppCommentLikeServiceImpl extends ServiceImpl<AppCommentLikeMapper, AppCommentLike>
        implements AppCommentLikeService {

    @Resource
    @Lazy
    private AppCommentService appCommentService;

    @Resource
    private SocialRedisHelper socialRedisHelper;

    /**
     * 切换评论点赞状态（已点赞则取消，未点赞则新增）
     * 使用 Redis 分布式锁保证同一用户对同一评论的并发操作串行化
     * 使用 @Transactional 保证点赞记录和评论likeCount的原子性更新
     *
     * @param commentId 评论id
     * @param userId    用户id
     * @return true=点赞成功, false=取消点赞成功
     * @throws BusinessException 评论不存在或操作频繁时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleCommentLike(Long commentId, Long userId) {
        // 校验评论是否存在
        AppComment comment = appCommentService.getById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "评论不存在");
        }

        String lockKey = SocialRedisHelper.commentLikeLockKey(commentId, userId);
        boolean locked = socialRedisHelper.tryLock(lockKey);
        if (!locked) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作频繁，请稍后重试");
        }
        try {
            QueryWrapper query = QueryWrapper.create()
                    .from(AppCommentLike.class)
                    .where("commentId = ? AND userId = ?", commentId, userId);
            AppCommentLike existing = getOne(query);

            if (existing != null) {
                // 取消点赞
                removeById(existing.getId());
                // 递减评论的likeCount（不低于0）
                UpdateChain.of(AppComment.class)
                        .setRaw("likeCount", "GREATEST(likeCount - 1, 0)")
                        .where("id = ?", commentId)
                        .update();
                return false;
            } else {
                // 新增点赞
                AppCommentLike commentLike = AppCommentLike.builder()
                        .commentId(commentId)
                        .userId(userId)
                        .build();
                save(commentLike);
                // 递增评论的likeCount
                UpdateChain.of(AppComment.class)
                        .setRaw("likeCount", "likeCount + 1")
                        .where("id = ?", commentId)
                        .update();
                return true;
            }
        } finally {
            socialRedisHelper.releaseLock(lockKey);
        }
    }

    /**
     * 检查用户是否已点赞指定评论
     *
     * @param commentId 评论id
     * @param userId    用户id
     * @return true=已点赞, false=未点赞
     */
    @Override
    public boolean hasLiked(Long commentId, Long userId) {
        if (userId == null) return false;
        QueryWrapper query = QueryWrapper.create()
                .from(AppCommentLike.class)
                .where("commentId = ? AND userId = ?", commentId, userId);
        return count(query) > 0;
    }
}
