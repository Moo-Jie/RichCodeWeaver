package com.rich.codeweaver.controller.social;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;

import com.rich.codeweaver.common.constant.UserConstant;
import com.rich.codeweaver.common.model.BaseResponse;
import com.rich.codeweaver.common.utils.ResultUtils;
import com.rich.codeweaver.common.exception.ErrorCode;
import com.rich.codeweaver.common.exception.ThrowUtils;
import com.rich.codeweaver.common.model.DeleteRequest;
import com.rich.codeweaver.model.dto.social.AppCommentAddRequest;
import com.rich.codeweaver.model.dto.social.AppCommentQueryRequest;
import com.rich.codeweaver.model.entity.User;
import com.rich.codeweaver.model.vo.AppCommentVO;
import com.rich.codeweaver.service.social.AppCommentLikeService;
import com.rich.codeweaver.service.social.AppCommentService;
import com.rich.codeweaver.service.user.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 产物评论控制器
 * 提供评论的增删查及评论点赞接口
 *
 * @author DuRuiChi
 * @since 2026-03-25
 */
@RestController
@RequestMapping("/social/comment")
public class AppCommentController {

    @Resource
    private AppCommentService appCommentService;

    @Resource
    private AppCommentLikeService appCommentLikeService;

    @Resource
    private UserService userService;

    /**
     * 新增评论
     *
     * @param addRequest 评论新增请求
     * @param request    HTTP请求对象
     * @return 新增评论的id
     * @author DuRuiChi
     */
    @PostMapping("/add")
    public BaseResponse<Long> addComment(@RequestBody AppCommentAddRequest addRequest,
                                         HttpServletRequest request) {
        ThrowUtils.throwIf(addRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(addRequest.getAppId() == null || addRequest.getAppId() <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(addRequest.getContent()), ErrorCode.PARAMS_ERROR, "评论内容不能为空");
        ThrowUtils.throwIf(addRequest.getContent().length() > 500, ErrorCode.PARAMS_ERROR, "评论内容不能超过500字");
        User loginUser = userService.getLoginUser(request);
        Long commentId = appCommentService.addComment(addRequest.getAppId(), loginUser.getId(), addRequest.getContent());
        return ResultUtils.success(commentId);
    }

    /**
     * 删除评论（仅评论作者或管理员可删除）
     *
     * @param deleteRequest 删除请求
     * @param request       HTTP请求对象
     * @return 操作成功
     * @author DuRuiChi
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteComment(@RequestBody DeleteRequest deleteRequest,
                                               HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        appCommentService.deleteComment(deleteRequest.getId(), loginUser.getId(), isAdmin);
        return ResultUtils.success(true);
    }

    /**
     * 分页查询产物评论列表
     *
     * @param queryRequest 分页查询请求
     * @param request      HTTP请求对象
     * @return 评论VO分页
     * @author DuRuiChi
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<AppCommentVO>> listCommentByPage(
            @RequestBody AppCommentQueryRequest queryRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(queryRequest.getAppId() == null || queryRequest.getAppId() <= 0, ErrorCode.PARAMS_ERROR);
        Long currentUserId = null;
        try {
            User loginUser = userService.getLoginUser(request);
            currentUserId = loginUser.getId();
        } catch (Exception ignored) {
            // 未登录用户也可查看评论
        }
        long pageNum = queryRequest.getPageNum();
        long pageSize = queryRequest.getPageSize();
        Page<AppCommentVO> page = appCommentService.listCommentByPage(
                queryRequest.getAppId(), currentUserId, pageNum, pageSize);
        return ResultUtils.success(page);
    }

    /**
     * 切换评论点赞状态
     *
     * @param commentId 评论id
     * @param request   HTTP请求对象
     * @return true=点赞成功, false=取消点赞成功
     * @author DuRuiChi
     */
    @PostMapping("/like")
    public BaseResponse<Boolean> toggleCommentLike(@RequestParam Long commentId, HttpServletRequest request) {
        ThrowUtils.throwIf(commentId == null || commentId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean liked = appCommentLikeService.toggleCommentLike(commentId, loginUser.getId());
        return ResultUtils.success(liked);
    }
}
