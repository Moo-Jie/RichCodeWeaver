package com.rich.richcodeweaver.controller;


import com.mybatisflex.core.paginate.Page;
import com.rich.richcodeweaver.annotation.AuthCheck;
import com.rich.richcodeweaver.constant.UserConstant;
import com.rich.richcodeweaver.exception.BusinessException;
import com.rich.richcodeweaver.exception.ErrorCode;
import com.rich.richcodeweaver.exception.ThrowUtils;
import com.rich.richcodeweaver.model.common.BaseResponse;
import com.rich.richcodeweaver.model.common.DeleteRequest;
import com.rich.richcodeweaver.model.dto.app.*;
import com.rich.richcodeweaver.model.entity.App;
import com.rich.richcodeweaver.model.entity.User;
import com.rich.richcodeweaver.model.vo.AppVO;
import com.rich.richcodeweaver.service.AppService;
import com.rich.richcodeweaver.service.UserService;
import com.rich.richcodeweaver.utils.ResultUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.io.File;

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

    @Resource
    private UserService userService;


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
     * @return com.rich.richcodeweaver.model.common.BaseResponse<java.lang.String>  部署成功后的 URL
     * @author DuRuiChi
     * @create 2025/8/10
     **/
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = appDeployRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");

        User loginUser = userService.getLoginUser(request);
        // 执行部署逻辑
        String deployUrl = appService.deployApp(appId, loginUser);
        return ResultUtils.success(deployUrl);
    }


    /**
     * 创建 AI 应用
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(appService.addApp(appAddRequest, request));
    }

    /**
     * 更新 AI 应用（用户只能更新自己的 AI 应用名称）
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
     */
    // 处理GET请求，路径为/get/vo
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
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(appService.listMyAppVOByPage(appQueryRequest, request));
    }

    /**
     * 分页获取星标 AI 应用列表
     */
    @PostMapping("/good/list/page/vo")
    public BaseResponse<Page<AppVO>> listStarAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        // 参数校验
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(appService.listStarAppVOByPage(appQueryRequest));
    }

    /**
     * 管理员删除 AI 应用
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
     */
    @GetMapping("/get/vo/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppVOByIdByAdmin(long id) {
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
     * 执行 AI 生成应用代码（SSE 流式)
     *
     * @param appId   应用id
     * @param message 用户消息
     * @param request 请求对象
     * @return 生成结果流
     */
    @GetMapping(value = "/gen/code/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatToGenCodeStream(@RequestParam Long appId, @RequestParam String message, HttpServletRequest request) {
        // 参数校验
        Long userId = userService.getLoginUser(request).getId();
        ThrowUtils.throwIf(appId == null || userId == null || message == null, ErrorCode.PARAMS_ERROR);
        // 执行 AI 生成应用代码
        return appService.aiChatAndGenerateCodeStream(appId, userId, message);
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
        Long userId = userService.getLoginUser(request).getId();
        ThrowUtils.throwIf(appId == null || userId == null || appId < 0 || userId < 0 || message == null, ErrorCode.PARAMS_ERROR);
        // 执行 AI 生成应用代码
        return appService.aiChatAndGenerateCode(appId, userId, message);
    }

}