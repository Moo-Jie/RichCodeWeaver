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
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 评论点赞 服务实现
 * 实现评论点赞/取消点赞的业务逻辑，同步更新评论的likeCount
 *
 * @author DuRuiChi
 * @since 2026-03-25
 */
@Service
public class AppCommentLikeServiceImpl extends ServiceImpl<AppCommentLikeMapper, AppCommentLike>
        implements AppCommentLikeService {

    @Resource
    @Lazy
    private AppCommentService appCommentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleCommentLike(Long commentId, Long userId) {
        // 校验评论是否存在
        AppComment comment = appCommentService.getById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "评论不存在");
        }

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
    }

    @Override
    public boolean hasLiked(Long commentId, Long userId) {
        if (userId == null) return false;
        QueryWrapper query = QueryWrapper.create()
                .from(AppCommentLike.class)
                .where("commentId = ? AND userId = ?", commentId, userId);
        return count(query) > 0;
    }
}
