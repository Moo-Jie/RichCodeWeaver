package com.rich.app.controller;

import com.rich.app.service.AppService;
import com.rich.app.service.DownloadCodeFileService;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.model.annotation.RateLimit;
import com.rich.model.entity.App;
import com.rich.model.enums.RateLimitTypeEnum;
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
 * @create 2025/9/4
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
     * @param appId    应用 ID
     * @param response 响应
     */
//    @RateLimit(type = RateLimitTypeEnum.API, rate = 30, window = 10)
    @GetMapping("/code/zip/{appId}")
    public void downloadCodeZipFile(@PathVariable Long appId,
//                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        // 参数校验
        ThrowUtils.throwIf(appId == null, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        // 应用校验
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 权限校验 TODO 不允许下载其他用户的代码时打开
//        User loginUser = InnerUserService.getLoginUser(request);
//        ThrowUtils.throwIf(loginUser.getId().equals(app.getUserId()), ErrorCode.NOT_FOUND_ERROR, "您无权下载此代码");
        // 下载代码文件
        downloadCodeFileService.downloadCodeZipFile(app, response);
    }
}
