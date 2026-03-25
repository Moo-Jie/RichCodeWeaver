package com.rich.social.controller;

import com.rich.client.innerService.InnerUserService;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.common.model.BaseResponse;
import com.rich.common.utils.ResultUtils;
import com.rich.model.entity.User;
import com.rich.model.vo.AppHotStatVO;
import com.rich.social.service.AppHotStatService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 产物热点统计控制器
 * 对外提供热点数据查询接口，与产物模块解耦
 *
 * @author DuRuiChi
 * @since 2026-03-25
 */
@RestController
@RequestMapping("/social/hotStat")
public class AppHotStatController {

    @Resource
    private AppHotStatService appHotStatService;

    /**
     * 获取产物热点统计数据（含当前用户的点赞/收藏状态）
     *
     * @param appId   产物id
     * @param request HTTP请求对象
     * @return 热点统计VO
     * @author DuRuiChi
     */
    @GetMapping("/get")
    public BaseResponse<AppHotStatVO> getHotStat(@RequestParam Long appId, HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR);
        Long userId = null;
        try {
            User loginUser = InnerUserService.getLoginUser(request);
            userId = loginUser.getId();
        } catch (Exception ignored) {
            // 未登录用户也可查看热点数据
        }
        AppHotStatVO vo = appHotStatService.getHotStatVO(appId, userId);
        return ResultUtils.success(vo);
    }
}
