package com.rich.app.controller;


import com.mybatisflex.core.paginate.Page;
import com.rich.app.service.AppService;
import com.rich.client.innerService.InnerFileService;
import com.rich.client.innerService.InnerUserService;
import com.rich.common.constant.UserConstant;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.common.model.BaseResponse;
import com.rich.common.model.DeleteRequest;
import com.rich.common.utils.ResultUtils;
import com.rich.model.annotation.AuthCheck;
import com.rich.model.dto.app.*;
import com.rich.model.entity.App;
import com.rich.model.entity.User;
import com.rich.model.vo.AppVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.File;

import static com.rich.common.constant.CacheConstant.STAR_APP_CACHE_NAME;

/**
 * AI 应用相关接口
 *
 * @author DuRuiChi
 * @create 2025/8/7
 **/
@RestController
@RequestMapping("/app")
public class AppController {

    @Resource
    private AppService appService;

    // 注入外部文件上传服务的代理对象
    @DubboReference
    private InnerFileService fileService;

    /**
     * 执行 AI 生成应用代码（SSE 流式)
     *
     * @param appId   应用id
     * @param message 用户消息
     * @param isAgent 是否开启 Agent 模式
     * @param request 请求对象
     * @return 生成结果流
     */
    @GetMapping(value = "/gen/code/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatToGenCodeStream(@RequestParam Long appId,
                                                             @RequestParam String message,
                                                             @RequestParam(defaultValue = "false") Boolean isAgent,
                                                             HttpServletRequest request) {
        // 参数校验
        Long userId = InnerUserService.getLoginUser(request).getId();
        ThrowUtils.throwIf(appId == null || userId == null || message == null, ErrorCode.PARAMS_ERROR);
        // 执行 AI 生成应用代码
        return appService.aiChatAndGenerateCodeStream(appId, userId, message, isAgent);
    }

    /**
     * 预览指定应用
     * URL：/api/app/{appId}[/{fileName}]
     *
     * @param appId   应用浏览标识，用于定位应用输出目录
     * @param request 请求对象
     * @return org.springframework.http.ResponseEntity<jakarta.annotation.Resource> 应用资源
     * @author DuRuiChi
     * @create 2025/8/9
     **/
    @GetMapping("/view/{appId}/**")
    public ResponseEntity<FileSystemResource> viewApp(@PathVariable Long appId, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR);
        // 响应服务静态资源
        return appService.serverStaticResource(appId, request);
    }

    /**
     * 部署应用
     *
     * @param appDeployRequest 部署请求参数
     * @param request          请求信息
     * @return com.rich.app.model.common.BaseResponse<java.lang.String>  部署成功后的 URL
     * @author DuRuiChi
     * @create 2025/8/10
     **/
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = appDeployRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");

        User loginUser = InnerUserService.getLoginUser(request);
        // 执行部署逻辑
        String deployUrl = appService.deployApp(appId, loginUser);
        return ResultUtils.success(deployUrl);
    }


    /**
     * 创建 AI 应用
     *
     * @param appAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(appService.addApp(appAddRequest, request));
    }

    /**
     * 更新 AI 应用（用户只能更新自己的 AI 应用名称）
     *
     * @param appUpdateRequest 更新请求参数
     * @param request          请求对象
     * @return com.rich.app.model.common.BaseResponse<java.lang.Boolean> 更新结果
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        // 参数校验
        if (appUpdateRequest == null || appUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(appService.updateApp(appUpdateRequest, request));
    }

    /**
     * 删除 AI 应用
     *
     * @param deleteRequest 删除请求参数
     * @param request       请求对象
     * @return com.rich.app.model.common.BaseResponse<java.lang.Boolean> 删除结果
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        // 参数校验
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(appService.deleteApp(deleteRequest, request));
    }

    /**
     * 根据 id 查询 AI 应用详情
     *
     * @param id 应用 id
     * @return com.rich.app.model.common.BaseResponse<com.rich.app.model.vo.AppVO> 应用详情
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(@RequestParam("id") long id) {
        // 参数校验
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 获取 AI 应用
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 分页获取当前用户创建的 AI 应用列表
     *
     * @param appQueryRequest 查询请求参数
     * @param request         请求对象
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(appService.listMyAppVOByPage(appQueryRequest, request));
    }

    /**
     * 分页获取星标 AI 应用列表
     *
     * @param appQueryRequest 查询请求参数
     * @return com.rich.app.model.common.BaseResponse<org.springframework.data.domain.Page < com.rich.app.model.vo.AppVO>> 星标应用列表
     */
//    @Cacheable(
//            value = STAR_APP_CACHE_NAME, // 缓存名
//            key = "T( com.rich.app.utils.RedisUtils).genKey(#appQueryRequest)" // 缓存 key 名
////            condition = "#appQueryRequest.pageNum <= 5" // 缓存条件
//    )
    @PostMapping("/good/list/page/vo")
    public BaseResponse<Page<AppVO>> listStarAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        // 参数校验
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(appService.listStarAppVOByPage(appQueryRequest));
    }

    /**
     * 管理员删除 AI 应用
     *
     * @param deleteRequest 删除请求参数
     * @return com.rich.app.model.common.BaseResponse<java.lang.Boolean> 删除结果
     */
    @PostMapping("/delete/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest) {
        // 参数校验
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取 AI 应用
        long id = deleteRequest.getId();
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 执行删除
        boolean result = appService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 管理员更新 AI 应用
     *
     * @param appAdminUpdateRequest 更新请求参数
     * @return com.rich.app.model.common.BaseResponse<java.lang.Boolean> 更新结果
     */
    @PostMapping("/update/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppAdminUpdateRequest appAdminUpdateRequest) {
        // 参数校验
        if (appAdminUpdateRequest == null || appAdminUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(appService.updateAppByAdmin(appAdminUpdateRequest));
    }

    /**
     * 管理员分页获取 AI 应用列表
     *
     * @param appQueryRequest 查询请求参数
     * @return com.rich.app.model.common.BaseResponse<org.springframework.data.domain.Page < com.rich.app.model.vo.AppVO>> 应用列表
     */
    @PostMapping("/list/page/vo/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppVOByPageByAdmin(@RequestBody AppQueryRequest appQueryRequest) {
        // 参数校验
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(appService.listAppVOByPageByAdmin(appQueryRequest));
    }

    /**
     * 管理员根据 id 获取 AI 应用详情
     *
     * @param id 应用 id
     * @return com.rich.app.model.common.BaseResponse<com.rich.app.model.vo.AppVO> 应用详情
     */
    @GetMapping("/get/vo/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppVOByIdByAdmin(@RequestParam("id") long id) {
        // 参数校验
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询 AI 应用实体
        App app = appService.getById(id);
        // 检查 AI 应用是否存在
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 转换为视图对象并返回
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 执行 AI 生成应用代码（非流式)
     *
     * @param appCodeGenRequest 应用代码生成请求
     * @param request           请求对象
     * @return 生成结果流
     */
    @GetMapping(value = "/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public File chatToGenCode(@RequestBody AppCodeGenRequest appCodeGenRequest, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appCodeGenRequest == null || request == null, ErrorCode.PARAMS_ERROR);
        Long appId = appCodeGenRequest.getAppId();
        String message = appCodeGenRequest.getMessage();
        Long userId = InnerUserService.getLoginUser(request).getId();
        ThrowUtils.throwIf(appId == null || userId == null || appId < 0 || userId < 0 || message == null, ErrorCode.PARAMS_ERROR);
        // 执行 AI 生成应用代码
        return appService.aiChatAndGenerateCode(appId, userId, message);
    }

    /**
     * 上传应用封面
     *
     * @param file    封面图片
     * @param appId   应用 ID
     * @param request 用户信息
     * @return com.rich.app.model.common.BaseResponse<java.lang.String>
     **/
    @PostMapping("/upload/cover")
    public BaseResponse<String> uploadAppCover(@RequestParam("file") MultipartFile file, @RequestParam("appId") Long appId, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR, "参数错误");
        User loginUser = InnerUserService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_FOUND_ERROR, "参数错误");
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "参数错误");
        // 权限校验
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "您无权更换他人应用封面");
        // 上传文件
        String url = fileService.upload(file);
        // 更新应用封面
        app.setCover(url);
        appService.updateById(app);
        // 返回图片URL
        return ResultUtils.success(url);
    }
}