package com.rich.social.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.rich.model.entity.AppComment;
import com.rich.model.vo.AppCommentVO;

/**
 * 产物评论 服务层
 * 提供评论的增删查及VO转换
 *
 * @author DuRuiChi
 * @since 2026-03-25
 */
public interface AppCommentService extends IService<AppComment> {

    /**
     * 新增评论
     *
     * @param appId   产物id
     * @param userId  用户id
     * @param content 评论内容
     * @return 新增评论的id
     */
    Long addComment(Long appId, Long userId, String content);

    /**
     * 删除评论（仅评论作者或管理员可删除）
     *
     * @param commentId 评论id
     * @param userId    当前用户id
     * @param isAdmin   是否为管理员
     */
    void deleteComment(Long commentId, Long userId, boolean isAdmin);

    /**
     * 分页查询评论列表（含评论者信息和当前用户的点赞状态）
     *
     * @param appId         产物id
     * @param currentUserId 当前用户id（可为null）
     * @param pageNum       页码
     * @param pageSize      每页大小
     * @return 评论VO分页
     */
    Page<AppCommentVO> listCommentByPage(Long appId, Long currentUserId, long pageNum, long pageSize);

    /**
     * 将评论实体转换为VO
     *
     * @param comment       评论实体
     * @param currentUserId 当前用户id
     * @return 评论VO
     */
    AppCommentVO getCommentVO(AppComment comment, Long currentUserId);
}
