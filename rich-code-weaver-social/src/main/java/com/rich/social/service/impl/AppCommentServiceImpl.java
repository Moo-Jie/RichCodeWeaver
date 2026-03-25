package com.rich.social.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.client.innerService.InnerUserService;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.model.entity.AppComment;
import com.rich.model.entity.User;
import com.rich.model.vo.AppCommentVO;
import com.rich.model.vo.UserVO;
import com.rich.social.mapper.AppCommentMapper;
import com.rich.social.service.AppCommentLikeService;
import com.rich.social.service.AppCommentService;
import com.rich.social.service.AppHotStatService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 产物评论 服务实现
 * 实现评论的增删查及VO转换，同步更新热点统计
 *
 * @author DuRuiChi
 * @since 2026-03-25
 */
@Service
public class AppCommentServiceImpl extends ServiceImpl<AppCommentMapper, AppComment>
        implements AppCommentService {

    @Resource
    private AppHotStatService appHotStatService;

    @Resource
    @Lazy
    private AppCommentLikeService appCommentLikeService;

    @DubboReference
    private InnerUserService innerUserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addComment(Long appId, Long userId, String content) {
        AppComment comment = AppComment.builder()
                .appId(appId)
                .userId(userId)
                .content(content)
                .likeCount(0)
                .build();
        save(comment);
        appHotStatService.incrementField(appId, "commentCount");
        return comment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, Long userId, boolean isAdmin) {
        AppComment comment = getById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "评论不存在");
        }
        // 仅评论作者或管理员可删除
        if (!comment.getUserId().equals(userId) && !isAdmin) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权删除该评论");
        }
        removeById(commentId);
        appHotStatService.decrementField(comment.getAppId(), "commentCount");
    }

    @Override
    public Page<AppCommentVO> listCommentByPage(Long appId, Long currentUserId, long pageNum, long pageSize) {
        QueryWrapper query = QueryWrapper.create()
                .from(AppComment.class)
                .where("appId = ?", appId)
                .orderBy("createTime DESC");
        Page<AppComment> commentPage = page(Page.of(pageNum, pageSize), query);

        List<AppCommentVO> voList = commentPage.getRecords().stream()
                .map(comment -> getCommentVO(comment, currentUserId))
                .collect(Collectors.toList());

        return new Page<>(voList, pageNum, pageSize, commentPage.getTotalRow());
    }

    @Override
    public AppCommentVO getCommentVO(AppComment comment, Long currentUserId) {
        if (comment == null) return null;
        AppCommentVO vo = new AppCommentVO();
        BeanUtil.copyProperties(comment, vo);

        // 填充评论者信息
        try {
            User user = innerUserService.getById(comment.getUserId());
            if (user != null) {
                UserVO userVO = innerUserService.getUserVO(user);
                vo.setUser(userVO);
            }
        } catch (Exception e) {
            // 用户信息获取失败不影响评论展示
        }

        // 填充当前用户的点赞状态
        if (currentUserId != null) {
            vo.setHasLiked(appCommentLikeService.hasLiked(comment.getId(), currentUserId));
        } else {
            vo.setHasLiked(false);
        }
        return vo;
    }
}
