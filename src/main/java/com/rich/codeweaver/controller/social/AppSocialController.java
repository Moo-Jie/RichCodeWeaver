package com.rich.codeweaver.controller.social;

import com.mybatisflex.core.paginate.Page;

import com.rich.codeweaver.common.model.BaseResponse;
import com.rich.codeweaver.common.utils.ResultUtils;
import com.rich.codeweaver.common.exception.ErrorCode;
import com.rich.codeweaver.common.exception.ThrowUtils;
import com.rich.codeweaver.model.dto.social.AppFavoriteQueryRequest;
import com.rich.codeweaver.model.entity.AppFavorite;
import com.rich.codeweaver.model.entity.User;
import com.rich.codeweaver.service.social.AppFavoriteService;
import com.rich.codeweaver.service.social.AppLikeService;
import com.rich.codeweaver.service.social.AppShareService;
import com.rich.codeweaver.service.user.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 产物社交行为控制器
 * 提供点赞、收藏、转发等社交行为接口
 *
 * @author DuRuiChi
 * @since 2026-03-25
 */
@RestController
@RequestMapping("/social")
public class AppSocialController {

    @Resource
    private AppLikeService appLikeService;

    @Resource
    private AppFavoriteService appFavoriteService;

    @Resource
    private AppShareService appShareService;

    @Resource
    private UserService userService;

    // ===== 点赞 =====

    /**
     * 切换产物点赞状态（已点赞则取消，未点赞则新增）
     *
     * @param appId   产物id
     * @param request HTTP请求对象
     * @return true=点赞成功, false=取消点赞成功
     * @author DuRuiChi
     */
    @PostMapping("/like")
    public BaseResponse<Boolean> toggleLike(@RequestParam Long appId, HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean liked = appLikeService.toggleLike(appId, loginUser.getId());
        return ResultUtils.success(liked);
    }

    // ===== 收藏 =====

    /**
     * 切换产物收藏状态（已收藏则取消，未收藏则新增）
     *
     * @param appId   产物id
     * @param request HTTP请求对象
     * @return true=收藏成功, false=取消收藏成功
     * @author DuRuiChi
     */
    @PostMapping("/favorite")
    public BaseResponse<Boolean> toggleFavorite(@RequestParam Long appId, HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean favorited = appFavoriteService.toggleFavorite(appId, loginUser.getId());
        return ResultUtils.success(favorited);
    }

    /**
     * 分页查询我的收藏列表
     *
     * @param queryRequest 分页查询请求
     * @param request      HTTP请求对象
     * @return 收藏记录分页
     * @author DuRuiChi
     */
    @PostMapping("/favorite/my/page")
    public BaseResponse<Page<AppFavorite>> listMyFavorites(
            @RequestBody AppFavoriteQueryRequest queryRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long pageNum = queryRequest.getPageNum();
        long pageSize = queryRequest.getPageSize();
        Page<AppFavorite> page = appFavoriteService.listMyFavorites(loginUser.getId(), pageNum, pageSize);
        return ResultUtils.success(page);
    }

    // ===== 转发 =====

    /**
     * 记录产物转发行为（转发不可撤销）
     *
     * @param appId   产物id
     * @param request HTTP请求对象
     * @return 操作成功
     * @author DuRuiChi
     */
    @PostMapping("/share")
    public BaseResponse<Boolean> doShare(@RequestParam Long appId, HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        appShareService.doShare(appId, loginUser.getId());
        return ResultUtils.success(true);
    }
}
