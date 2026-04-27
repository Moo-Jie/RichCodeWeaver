package com.rich.social.controller;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.rich.client.innerService.InnerUserService;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.common.model.BaseResponse;
import com.rich.common.utils.ResultUtils;
import com.rich.model.dto.social.CommunityPostAddRequest;
import com.rich.model.dto.social.CommunityPostQueryRequest;
import com.rich.model.dto.social.CommunityReplyAddRequest;
import com.rich.model.dto.social.CommunityReplyQueryRequest;
import com.rich.model.entity.User;
import com.rich.model.enums.UserRoleEnum;
import com.rich.model.vo.CommunityPostVO;
import com.rich.model.vo.CommunityReplyVO;
import com.rich.social.service.CommunityPostLikeService;
import com.rich.social.service.CommunityPostService;
import com.rich.social.service.CommunityReplyLikeService;
import com.rich.social.service.CommunityReplyService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 交流社区控制器。
 * 统一对外提供帖子、回复、点赞等社区接口。
 *
 * @author DuRuiChi
 * @since 2026-04-11
 */
@RestController
@RequestMapping("/social/community")
public class CommunityController {

    @Resource
    private CommunityPostService communityPostService;

    @Resource
    private CommunityReplyService communityReplyService;

    @Resource
    private CommunityPostLikeService communityPostLikeService;

    @Resource
    private CommunityReplyLikeService communityReplyLikeService;

    /**
     * 新增社区帖子。
     */
    @PostMapping("/post/add")
    public BaseResponse<Long> addPost(@RequestBody CommunityPostAddRequest addRequest,
                                      HttpServletRequest request) {
        ThrowUtils.throwIf(addRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(addRequest.getTitle()), ErrorCode.PARAMS_ERROR, "标题不能为空");
        ThrowUtils.throwIf(addRequest.getTitle().length() > 200, ErrorCode.PARAMS_ERROR, "标题不能超过200字");
        ThrowUtils.throwIf(StrUtil.isBlank(addRequest.getContent()), ErrorCode.PARAMS_ERROR, "内容不能为空");
        ThrowUtils.throwIf(addRequest.getContent().length() > 10000, ErrorCode.PARAMS_ERROR, "内容不能超过10000字");
        User loginUser = InnerUserService.getLoginUser(request);
        String category = StrUtil.isBlank(addRequest.getCategory()) ? "discuss" : addRequest.getCategory();
        Long postId = communityPostService.addPost(addRequest.getTitle().trim(), addRequest.getContent().trim(), category, loginUser.getId());
        return ResultUtils.success(postId);
    }

    /**
     * 分页查询社区帖子列表。
     * 未登录用户也可以访问，因此当前用户 id 允许为空。
     */
    @PostMapping("/post/list/page")
    public BaseResponse<Page<CommunityPostVO>> listPostByPage(@RequestBody CommunityPostQueryRequest queryRequest,
                                                              HttpServletRequest request) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        Long currentUserId = null;
        try {
            User loginUser = InnerUserService.getLoginUser(request);
            currentUserId = loginUser.getId();
        } catch (Exception ignored) {
        }
        Page<CommunityPostVO> page = communityPostService.listPostByPage(
                queryRequest.getCategory(),
                queryRequest.getSearchText(),
                queryRequest.getSortField(),
                queryRequest.getSortOrder(),
                currentUserId,
                queryRequest.getPageNum(),
                queryRequest.getPageSize()
        );
        return ResultUtils.success(page);
    }

    /**
     * 获取帖子详情视图对象。
     * 支持通过 increaseView 参数控制是否累计浏览量。
     */
    @GetMapping("/post/get/vo")
    public BaseResponse<CommunityPostVO> getPostVO(@RequestParam Long id,
                                                   @RequestParam(required = false, defaultValue = "true") Boolean increaseView,
                                                   HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Long currentUserId = null;
        try {
            User loginUser = InnerUserService.getLoginUser(request);
            currentUserId = loginUser.getId();
        } catch (Exception ignored) {
        }
        if (Boolean.TRUE.equals(increaseView)) {
            communityPostService.increaseViewCount(id);
        }
        CommunityPostVO postVO = communityPostService.getPostVO(id, currentUserId);
        return ResultUtils.success(postVO);
    }

    /**
     * 切换帖子点赞状态。
     * 已点赞时取消点赞，未点赞时新增点赞。
     */
    @PostMapping("/post/like")
    public BaseResponse<Boolean> togglePostLike(@RequestParam Long postId, HttpServletRequest request) {
        ThrowUtils.throwIf(postId == null || postId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = InnerUserService.getLoginUser(request);
        boolean liked = communityPostLikeService.togglePostLike(postId, loginUser.getId());
        return ResultUtils.success(liked);
    }

    /**
     * 设置帖子置顶状态。
     * 仅管理员允许操作。
     */
    @PostMapping("/post/top")
    public BaseResponse<Boolean> updatePostTopStatus(@RequestParam Long postId,
                                                     @RequestParam Integer isTop,
                                                     HttpServletRequest request) {
        ThrowUtils.throwIf(postId == null || postId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(isTop == null || (isTop != 0 && isTop != 1), ErrorCode.PARAMS_ERROR);
        User loginUser = InnerUserService.getLoginUser(request);
        if (!UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅管理员可以设置帖子置顶");
        }
        communityPostService.updateTopStatus(postId, isTop);
        return ResultUtils.success(true);
    }

    /**
     * 删除帖子。
     * 仅帖子作者或管理员允许操作。
     */
    @PostMapping("/post/delete")
    public BaseResponse<Boolean> deletePost(@RequestParam Long postId, HttpServletRequest request) {
        ThrowUtils.throwIf(postId == null || postId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = InnerUserService.getLoginUser(request);
        com.rich.model.entity.CommunityPost post = communityPostService.getById(postId);
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        if (!post.getUserId().equals(loginUser.getId())
                && !UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅作者或管理员可以删除帖子");
        }
        communityPostService.deletePost(postId);
        return ResultUtils.success(true);
    }

    /**
     * 新增帖子回复。
     */
    @PostMapping("/reply/add")
    public BaseResponse<Long> addReply(@RequestBody CommunityReplyAddRequest addRequest,
                                       HttpServletRequest request) {
        ThrowUtils.throwIf(addRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(addRequest.getPostId() == null || addRequest.getPostId() <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(addRequest.getContent()), ErrorCode.PARAMS_ERROR, "回复内容不能为空");
        ThrowUtils.throwIf(addRequest.getContent().length() > 5000, ErrorCode.PARAMS_ERROR, "回复内容不能超过5000字");
        User loginUser = InnerUserService.getLoginUser(request);
        Long replyId = communityReplyService.addReply(addRequest.getPostId(), loginUser.getId(), addRequest.getContent().trim());
        return ResultUtils.success(replyId);
    }

    /**
     * 分页查询指定帖子的回复列表。
     */
    @PostMapping("/reply/list/page")
    public BaseResponse<Page<CommunityReplyVO>> listReplyByPage(@RequestBody CommunityReplyQueryRequest queryRequest,
                                                                HttpServletRequest request) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(queryRequest.getPostId() == null || queryRequest.getPostId() <= 0, ErrorCode.PARAMS_ERROR);
        Long currentUserId = null;
        try {
            User loginUser = InnerUserService.getLoginUser(request);
            currentUserId = loginUser.getId();
        } catch (Exception ignored) {
        }
        Page<CommunityReplyVO> page = communityReplyService.listReplyByPage(
                queryRequest.getPostId(),
                currentUserId,
                queryRequest.getPageNum(),
                queryRequest.getPageSize()
        );
        return ResultUtils.success(page);
    }

    /**
     * 切换回复点赞状态。
     */
    @PostMapping("/reply/like")
    public BaseResponse<Boolean> toggleReplyLike(@RequestParam Long replyId, HttpServletRequest request) {
        ThrowUtils.throwIf(replyId == null || replyId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = InnerUserService.getLoginUser(request);
        boolean liked = communityReplyLikeService.toggleReplyLike(replyId, loginUser.getId());
        return ResultUtils.success(liked);
    }
}
