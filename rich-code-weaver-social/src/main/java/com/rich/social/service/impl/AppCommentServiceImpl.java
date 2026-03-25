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
 * @create 2026-03-25
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

    /**
     * 添加产物评论
     * 使用 @Transactional 保证评论记录和热点统计的原子性更新
     *
     * @param appId   产物id
     * @param userId  用户id
     * @param content 评论内容
     * @return 新创建的评论id
     */
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

    /**
     * 删除产物评论（仅评论作者或管理员可删除）
     * 使用 @Transactional 保证评论删除和热点统计的原子性更新
     *
     * @param commentId 评论id
     * @param userId    当前用户id
     * @param isAdmin   是否为管理员
     * @throws BusinessException 评论不存在或无权删除时抛出
     */
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

    /**
     * 分页查询产物评论列表
     *
     * @param appId         产物id
     * @param currentUserId 当前用户id（可为null，未登录用户）
     * @param pageNum       页码（从1开始）
     * @param pageSize      每页数量
     * @return 评论VO分页，按创建时间降序排列，包含用户信息和点赞状态
     */
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

    /**
     * 将评论实体转换为VO（包含用户信息和点赞状态）
     *
     * @param comment       评论实体
     * @param currentUserId 当前用户id（可为null，未登录用户）
     * @return 评论VO，包含评论者信息和当前用户的点赞状态
     */
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
