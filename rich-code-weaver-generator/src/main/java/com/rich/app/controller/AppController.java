package com.rich.app.controller;

import com.mybatisflex.core.paginate.Page;
import com.rich.ai.service.AiPromptOptimizationService;
import com.rich.app.service.AppService;
import com.rich.app.service.TaskExecutionService;
import com.rich.client.innerService.InnerCollaboratorService;
import com.rich.client.innerService.InnerFileService;
import com.rich.client.innerService.InnerUserService;
import com.rich.common.constant.AppOwnershipConstant;
import com.rich.common.constant.UserConstant;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.common.model.BaseResponse;
import com.rich.common.model.DeleteRequest;
import com.rich.common.utils.ResultUtils;
import com.rich.model.annotation.AuthCheck;
import com.rich.model.dto.app.*;
import com.rich.model.entity.App;
import com.rich.model.entity.TaskExecution;
import com.rich.model.entity.User;
import com.rich.model.vo.AppVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

/**
 * AI 产物相关接口
 *
 * @author DuRuiChi
 * @create 2025/12/7
 **/
@RestController
@RequestMapping("/generator/app")
public class AppController {

    /**
     * 布尔参数默认值
     */
    private static final String DEFAULT_BOOLEAN_FALSE = "false";

    /**
     * 用户相关产物默认查询数量
     */
    private static final String DEFAULT_USER_RELATED_PAGE_SIZE = "8";

    /**
     * 用户相关产物最大查询数量
     */
    private static final long MAX_USER_RELATED_PAGE_SIZE = 20L;

    /**
     * 素材 ID 格式错误提示
     */
    private static final String INVALID_MATERIAL_ID_MESSAGE = "素材ID格式错误";

    @Resource
    private AppService appService;

    @Resource
    private AiPromptOptimizationService aiPromptOptimizationService;

    @Resource
    private TaskExecutionService taskExecutionService;

    // 注入外部文件上传服务的代理对象
    @DubboReference
    private InnerFileService fileService;

    @DubboReference
    private InnerCollaboratorService collaboratorService;

    /**
     * 执行 AI 生成产物代码（SSE 流式，支持断线重连）
     * 使用工作流分布执行节点模式
     *
     * @param appId       产物id
     * @param message     用户消息
     * @param materialIds 选中的素材ID列表（逗号分隔，可选）
     * @param isWorkflow  是否开启 Agent 模式（前端参数，暂时保留用于未来 Agent 模式）
     * @param reconnect   是否为重连请求
     * @param lastEventId 最后接收到的事件ID
     * @param request     请求对象
     * @return 生成结果流
     */
    @GetMapping(value = "/gen/code/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatToGenCodeStream(@RequestParam Long appId,
                                                             @RequestParam String message,
                                                             @RequestParam(required = false) String materialIds,
                                                             @RequestParam(defaultValue = DEFAULT_BOOLEAN_FALSE) Boolean isWorkflow,
                                                             @RequestParam(defaultValue = DEFAULT_BOOLEAN_FALSE) Boolean reconnect,
                                                             @RequestParam(required = false) String lastEventId,
                                                             HttpServletRequest request) {
        User loginUser = InnerUserService.getLoginUser(request);
        Long userId = loginUser.getId();

        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "产物ID无效");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID无效");
        ThrowUtils.throwIf(message == null || message.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "消息内容不能为空");

        List<Long> materialIdList = parseMaterialIds(materialIds);

        return appService.aiChatAndGenerateCodeStreamWithReconnect(
                appId, userId, message.trim(), materialIdList, isWorkflow, lastEventId, reconnect);
    }

    /**
     * 预览指定产物
     * URL：/api/app/{appId}[/{fileName}]
     *
     * @param appId   产物浏览标识，用于定位产物输出目录
     * @param request 请求对象
     * @return 产物资源
     * @author DuRuiChi
     * @create 2026/8/9
     **/
    @GetMapping("/view/{appId}/**")
    public ResponseEntity<FileSystemResource> viewApp(@PathVariable Long appId, HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "产物ID无效");
        return appService.serverStaticResource(appId, request);
    }

    /**
     * 部署产物(同步版本，保持向后兼容)
     * HTML/MULTI_FILE直接部署
     * Vue项目同步构建(阻塞等待)
     *
     * @param appDeployRequest 部署请求参数
     * @param request          请求信息
     * @return 部署成功后的 URL
     * @author DuRuiChi
     * @create 2026/8/10
     **/
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR, "部署请求参数不能为空");

        Long appId = appDeployRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "产物ID无效");

        User loginUser = InnerUserService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");

        String deployUrl = appService.deployApp(appId, loginUser);
        return ResultUtils.success(deployUrl);
    }

    /**
     * 部署产物(异步版本，推荐使用)
     * Vue项目发送到消息队列异步构建
     * HTML/MULTI_FILE直接部署
     *
     * @param appDeployRequest 部署请求参数
     * @param request          请求信息
     * @return 部署URL(Vue项目为临时URL，需通过WebSocket获取最终状态)
     * @author DuRuiChi
     * @create 2026/5/6
     **/
    @PostMapping("/deploy/async")
    public BaseResponse<String> deployAppAsync(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR, "部署请求参数不能为空");

        Long appId = appDeployRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "产物ID无效");

        User loginUser = InnerUserService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");

        String deployUrl = appService.deployAppAsync(appId, loginUser);
        return ResultUtils.success(deployUrl);
    }

    /**
     * 刷新产物(同步版本，保持向后兼容)
     * 仅重新构建，不执行部署
     *
     * @param appDeployRequest 刷新请求参数
     * @param request          请求信息
     * @return 刷新是否成功
     */
    @PostMapping("/refresh")
    public BaseResponse<Boolean> refreshApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR, "刷新请求参数不能为空");

        Long appId = appDeployRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "产物ID无效");

        User loginUser = InnerUserService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");

        Boolean result = appService.refreshApp(appId, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 刷新产物(异步版本，推荐使用)
     * 重新构建Vue项目，发送到消息队列
     *
     * @param appDeployRequest 刷新请求参数
     * @param request          请求信息
     * @return 是否提交成功
     * @author DuRuiChi
     * @create 2026/5/6
     */
    @PostMapping("/refresh/async")
    public BaseResponse<Boolean> refreshAppAsync(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR, "刷新请求参数不能为空");

        Long appId = appDeployRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "产物ID无效");

        User loginUser = InnerUserService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");

        Boolean result = appService.refreshAppAsync(appId, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 查询任务状态
     * 用于前端轮询或WebSocket连接失败时的降级方案
     *
     * @param appId 产物ID
     * @return 任务执行记录
     * @author DuRuiChi
     * @create 2026/5/6
     */
    @GetMapping("/task/status")
    public BaseResponse<TaskExecution> getTaskStatus(@RequestParam Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "产物ID无效");
        TaskExecution task = taskExecutionService.getLatestByAppId(appId);
        return ResultUtils.success(task);
    }

    /**
     * 创建 AI 产物
     *
     * @param appAddRequest 产物添加请求参数
     * @param request       请求对象
     * @return 新创建的产物ID
     * @author DuRuiChi
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR, "产物添加请求参数不能为空");
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "请求对象不能为空");

        Long newAppId = appService.addApp(appAddRequest, request);
        return ResultUtils.success(newAppId);
    }

    /**
     * 更新 AI 产物（用户只能更新自己的 AI 产物名称）
     *
     * @param appUpdateRequest 更新请求参数
     * @param request          请求对象
     * @return 更新结果
     * @author DuRuiChi
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appUpdateRequest == null, ErrorCode.PARAMS_ERROR, "更新请求参数不能为空");
        ThrowUtils.throwIf(appUpdateRequest.getId() == null || appUpdateRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR, "产物ID无效");

        boolean updateResult = appService.updateApp(appUpdateRequest, request);
        return ResultUtils.success(updateResult);
    }

    /**
     * 删除 AI 产物
     *
     * @param deleteRequest 删除请求参数
     * @param request       请求对象
     * @return 删除结果
     * @author DuRuiChi
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR, "删除请求参数不能为空");
        ThrowUtils.throwIf(deleteRequest.getId() == null || deleteRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR, "产物ID无效");

        boolean deleteResult = appService.deleteApp(deleteRequest, request);
        return ResultUtils.success(deleteResult);
    }

    /**
     * 根据 id 查询 AI 产物详情
     *
     * @param id 产物 id
     * @return 产物详情
     * @author DuRuiChi
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(@RequestParam("id") long id, HttpServletRequest request) {
        // 参数校验：验证产物ID有效性
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR, "产物ID必须大于0");

        // 查询产物实体
        App app = getAppOrThrow(id);

        // 转换为视图对象并返回
        AppVO appVO = appService.getAppVO(app);
        fillOwnershipTypeIfPossible(appVO, app, request);
        return ResultUtils.success(appVO);
    }

    /**
     * 分页获取当前用户的 AI 产物列表
     *
     * @param appQueryRequest 查询请求参数
     * @param request         请求对象
     * @return 产物分页结果
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        // 参数校验：验证查询请求参数不为空
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR, "查询请求参数不能为空");

        // 查询当前用户的产物列表（分页）
        Page<AppVO> appPage = appService.listMyAppVOByPage(appQueryRequest, request);
        return ResultUtils.success(appPage);
    }

    /**
     * 查询用户相关的产物列表
     *
     * @param userId 用户 ID
     * @param pageSize 查询数量
     * @return 产物列表
     */
    @GetMapping("/user/related/list")
    public BaseResponse<List<AppVO>> listUserRelatedApps(@RequestParam Long userId,
                                                         @RequestParam(defaultValue = DEFAULT_USER_RELATED_PAGE_SIZE) long pageSize) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID无效");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > MAX_USER_RELATED_PAGE_SIZE, ErrorCode.PARAMS_ERROR, "查询数量必须在1-20之间");
        return ResultUtils.success(appService.listUserRelatedApps(userId, pageSize));
    }

    /**
     * 分页获取星标 AI 产物列表
     *
     * @param appQueryRequest 查询请求参数
     * @return 星标产物列表
     */
    @PostMapping("/good/list/page/vo")
    public BaseResponse<Page<AppVO>> listStarAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        // 参数校验：验证查询请求参数不为空
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR, "查询请求参数不能为空");

        // 查询星标（热门）产物列表（分页）
        Page<AppVO> starAppPage = appService.listStarAppVOByPage(appQueryRequest);
        return ResultUtils.success(starAppPage);
    }

    /**
     * 管理员分页获取 AI 产物列表
     *
     * @param appQueryRequest 查询请求参数
     * @return 产物列表
     */
    @PostMapping("/list/page/vo/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppVOByPageByAdmin(@RequestBody AppQueryRequest appQueryRequest) {
        // 参数校验
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(appService.listAppVOByPageByAdmin(appQueryRequest));
    }

    /**
     * 管理员更新 AI 产物
     *
     * @param appAdminUpdateRequest 更新请求参数
     * @return 更新结果
     * @author DuRuiChi
     * @description 管理员更新 AI 产物，支持更新产物名称、描述、标签等信息
     */
    @PostMapping("/update/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppAdminUpdateRequest appAdminUpdateRequest) {
        // 参数校验：验证管理员更新请求参数和产物ID
        ThrowUtils.throwIf(appAdminUpdateRequest == null, ErrorCode.PARAMS_ERROR, "更新请求参数不能为空");
        ThrowUtils.throwIf(appAdminUpdateRequest.getId() == null || appAdminUpdateRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR, "产物ID无效");

        // 执行管理员更新逻辑
        boolean updateResult = appService.updateAppByAdmin(appAdminUpdateRequest);
        return ResultUtils.success(updateResult);
    }

    /**
     * 管理员根据 id 获取 AI 产物详情
     *
     * @param id 产物 id
     * @return 产物详情
     * @author DuRuiChi
     * @description 管理员根据 id 获取 AI 产物详情，返回结果包含产物 ID、名称、描述、创建时间等信息
     */
    @GetMapping("/get/vo/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppVOByIdByAdmin(@RequestParam("id") long id) {
        // 参数校验
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR, "产物ID必须大于0");
        // 查询 AI 产物实体
        App app = getAppOrThrow(id);
        // 转换为视图对象并返回
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 上传产物封面
     *
     * @param file    封面图片
     * @param appId   产物 ID
     * @param request 用户信息
     * @return 上传后的图片URL
     * @author DuRuiChi
     * @description 上传产物封面，支持jpg、png等格式图片
     */
    @PostMapping("/upload/cover")
    public BaseResponse<String> uploadAppCover(@RequestParam("file") MultipartFile file,
                                               @RequestParam("appId") Long appId,
                                               HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "产物ID无效");

        // 获取当前登录用户
        User loginUser = InnerUserService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");

        // 查询产物信息
        App app = getAppOrThrow(appId);

        // 权限校验：只能更新自己的产物封面
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "您无权更换他人产物封面");

        // 上传文件到文件服务
        String uploadedUrl = fileService.upload(file);
        ThrowUtils.throwIf(uploadedUrl == null || uploadedUrl.trim().isEmpty(),
                ErrorCode.SYSTEM_ERROR, "文件上传失败");

        // 更新产物封面URL
        app.setCover(uploadedUrl);
        boolean updateSuccess = appService.updateById(app);
        ThrowUtils.throwIf(!updateSuccess, ErrorCode.SYSTEM_ERROR, "更新产物封面失败");

        // 返回上传后的图片URL
        return ResultUtils.success(uploadedUrl);
    }

    /**
     * 优化用户提示词
     *
     * @param userPrompt 用户原始提示词
     * @return 优化后的提示词
     * @author DuRuiChi
     */
    @PostMapping("/optimize/prompt")
    public BaseResponse<String> optimizePrompt(@RequestBody String userPrompt) {
        // 参数校验
        ThrowUtils.throwIf(userPrompt == null || userPrompt.trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "提示词不能为空");
        // 调用 AI 优化服务
        String optimizedPrompt = aiPromptOptimizationService.optimizePrompt(userPrompt.trim());
        // 返回优化后的提示词
        return ResultUtils.success(optimizedPrompt);
    }

    /**
     * 解析素材 ID 列表
     *
     * @param materialIds 素材 ID 字符串
     * @return 素材 ID 列表
     */
    private List<Long> parseMaterialIds(String materialIds) {
        if (materialIds == null || materialIds.trim().isEmpty()) {
            return null;
        }
        try {
            return Arrays.stream(materialIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .toList();
        } catch (NumberFormatException e) {
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, INVALID_MATERIAL_ID_MESSAGE);
            return null;
        }
    }

    /**
     * 查询产物，不存在时直接抛异常
     *
     * @param appId 产物 ID
     * @return 产物实体
     */
    private App getAppOrThrow(Long appId) {
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "产物不存在");
        return app;
    }

    /**
     * 按当前登录身份补充产物归属类型
     * 这里只做展示信息补充，异常时维持原行为静默跳过
     *
     * @param appVO 产物视图对象
     * @param app   产物实体
     * @param request 请求对象
     */
    private void fillOwnershipTypeIfPossible(AppVO appVO, App app, HttpServletRequest request) {
        try {
            User loginUser = InnerUserService.getLoginUser(request);
            if (loginUser == null || loginUser.getId() == null) {
                return;
            }
            if (loginUser.getId().equals(app.getUserId())) {
                appVO.setOwnershipType(AppOwnershipConstant.OWNERSHIP_MINE);
            } else if (collaboratorService.isCollaborator(app.getId(), loginUser.getId())) {
                appVO.setOwnershipType(AppOwnershipConstant.OWNERSHIP_COLLABORATOR);
            }
        } catch (Exception ignored) {
        }
    }
}