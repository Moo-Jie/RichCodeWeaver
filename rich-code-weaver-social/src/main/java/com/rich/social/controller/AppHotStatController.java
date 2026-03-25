package com.rich.social.controller;

import com.mybatisflex.core.paginate.Page;
import com.rich.client.innerService.InnerUserService;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.common.model.BaseResponse;
import com.rich.common.utils.ResultUtils;
import com.rich.model.entity.AppHotStat;
import com.rich.model.entity.User;
import com.rich.model.vo.AppHotStatVO;
import com.rich.social.service.AppHotStatService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    /**
     * 分页查询热门产物列表（按综合热度得分降序）
     *
     * @param params 请求参数（pageNum, pageSize）
     * @return 分页热点统计数据
     * @author DuRuiChi
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<AppHotStat>> listHotApps(@RequestBody Map<String, Object> params) {
        long pageNum = params.get("pageNum") != null ? Long.parseLong(params.get("pageNum").toString()) : 1;
        long pageSize = params.get("pageSize") != null ? Long.parseLong(params.get("pageSize").toString()) : 10;
        ThrowUtils.throwIf(pageSize > 50, ErrorCode.PARAMS_ERROR, "每页数量不能超过50");
        Page<AppHotStat> page = appHotStatService.listHotApps(pageNum, pageSize);
        return ResultUtils.success(page);
    }
}
