package com.rich.app.controller;

import com.rich.app.service.AppService;
import com.rich.app.service.DownloadCodeFileService;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.model.entity.App;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 下载代码文件相关接口
 *
 * @author DuRuiChi
 * @create 2026/1/4
 **/
@RestController
@RequestMapping("/download")
public class DownloadCodeFileController {
    @Resource
    private AppService appService;

    @Autowired
    private DownloadCodeFileService downloadCodeFileService;

    /**
     * 下载代码文件
     *
     * @param appId    产物 ID
     * @param response 响应对象
     * @author DuRuiChi
     */
//    @RateLimit(type = RateLimitTypeEnum.API, rate = 30, window = 10)
    @GetMapping("/code/zip/{appId}")
    public void downloadCodeZipFile(@PathVariable Long appId,
//                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        // 参数校验：验证产物ID有效性
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "产物ID无效");
        
        // 查询产物信息
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "产物不存在");
        
        // 权限校验（预留）
        // TODO: 开启权限控制时，只允许下载自己的代码
//        User loginUser = InnerUserService.getLoginUser(request);
//        ThrowUtils.throwIf(!loginUser.getId().equals(app.getUserId()), 
//                ErrorCode.NO_AUTH_ERROR, "您无权下载此代码");
        
        // 执行代码文件下载（打包为ZIP并通过HTTP响应返回）
        downloadCodeFileService.downloadCodeZipFile(app, response);
    }
}
