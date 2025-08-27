package com.rich.richcodeweaver.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.richcodeweaver.constant.AppConstant;
import com.rich.richcodeweaver.constant.UserConstant;
import com.rich.richcodeweaver.exception.BusinessException;
import com.rich.richcodeweaver.exception.ErrorCode;
import com.rich.richcodeweaver.exception.ThrowUtils;
import com.rich.richcodeweaver.mapper.AppMapper;
import com.rich.richcodeweaver.model.common.DeleteRequest;
import com.rich.richcodeweaver.model.dto.app.AppAddRequest;
import com.rich.richcodeweaver.model.dto.app.AppAdminUpdateRequest;
import com.rich.richcodeweaver.model.dto.app.AppQueryRequest;
import com.rich.richcodeweaver.model.dto.app.AppUpdateRequest;
import com.rich.richcodeweaver.model.entity.App;
import com.rich.richcodeweaver.model.entity.User;
import com.rich.richcodeweaver.model.enums.ChatHistoryTypeEnum;
import com.rich.richcodeweaver.model.enums.CodeGeneratorTypeEnum;
import com.rich.richcodeweaver.model.vo.AppVO;
import com.rich.richcodeweaver.model.vo.UserVO;
import com.rich.richcodeweaver.service.AppService;
import com.rich.richcodeweaver.service.ChatHistoryService;
import com.rich.richcodeweaver.service.UserService;
import com.rich.richcodeweaver.utiles.aiUtils.AIGenerateCodeAndSaveToFileUtils;
import com.rich.richcodeweaver.utiles.aiUtils.streamHandle.StreamHandlerExecutor;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.HandlerMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rich.richcodeweaver.constant.AppConstant.CODE_OUTPUT_ROOT_DIR;

/**
 * AI 应用 服务层实现
 *
 * @author DuRuiChi
 * @create 2025/8/5
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private AppService appService;

    @Resource
    private AIGenerateCodeAndSaveToFileUtils aiGenerateCodeAndSaveToFileUtils;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    /**
     * 管理员执行 AI 对话并并生成代码(流式)
     *
     * @param appId   AI 应用id
     * @param userId  用户id
     * @param message 对话消息
     * @return 代码流
     * @author DuRuiChi
     * @create 2025/8/8
     **/
    @Override
    public Flux<ServerSentEvent<String>> aiChatAndGenerateCodeStream(Long appId, Long userId, String message) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || userId == null || appId < 0 || userId < 0 || message == null, ErrorCode.PARAMS_ERROR);
        // 查询 AI 应用
        App app = appService.getById(appId);
        // 校验 AI 应用是否存在
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 是否为当前用户所属 AI 应用
        ThrowUtils.throwIf(!app.getUserId().equals(userId), ErrorCode.FORBIDDEN_ERROR);
        // 获取生成类型
        CodeGeneratorTypeEnum type = CodeGeneratorTypeEnum.getEnumByValue(app.getCodeGenType());
        if (type == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码生成类型为空");
        }
        // 保存用户消息
        boolean isSaveMsg = chatHistoryService.addChatMessage(appId, message, ChatHistoryTypeEnum.USER.getValue(), userId);
        ThrowUtils.throwIf(!isSaveMsg, ErrorCode.OPERATION_ERROR, "保存用户消息失败");
        // 用于收集 AI 响应内容的 StringBuilder
        StringBuilder aiResponseBuilder = new StringBuilder();
        // 调用 AI 基础响应流
        Flux<String> stringFlux = aiGenerateCodeAndSaveToFileUtils.aiGenerateAndSaveCodeStream(message, type, appId);
        // 处理 AI 响应流
        return streamHandlerExecutor.executeStreamHandler(stringFlux, chatHistoryService, appId, userId, type);
    }

    /**
     * 管理员执行 AI 对话并并生成代码(非流式)
     *
     * @param appId   AI 应用id
     * @param userId  用户id
     * @param message 对话消息
     * @return 代码流
     * @author DuRuiChi
     * @create 2025/8/8
     **/
    @Override
    public File aiChatAndGenerateCode(Long appId, Long userId, String message) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || userId == null || appId < 0 || userId < 0 || message == null, ErrorCode.PARAMS_ERROR);
        // 查询 AI 应用
        App app = appService.getById(appId);
        // 校验 AI 应用是否存在
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 是否为当前用户所属 AI 应用
        ThrowUtils.throwIf(!app.getUserId().equals(userId), ErrorCode.FORBIDDEN_ERROR);
        // 获取生成类型
        CodeGeneratorTypeEnum type = CodeGeneratorTypeEnum.getEnumByValue(app.getCodeGenType());
        // 调用 AI 响应代码流
        return aiGenerateCodeAndSaveToFileUtils.aiGenerateAndSaveCode(message, type, appId);
    }

    /**
     * 预览指定应用
     *
     * @param appId   应用 ID
     * @param request 请求对象
     * @return org.springframework.http.ResponseEntity<jakarta.annotation.Resource> 应用资源
     * @author DuRuiChi
     * @create 2025/8/8
     **/
    @Override
    public ResponseEntity<FileSystemResource> serverStaticResource(Long appId, HttpServletRequest request) {
        try {
            // 构建 viewKey，用于定位应用输出目录
            App app = appService.getById(appId);
            String codeGenType = app.getCodeGenType();
            String viewKey = codeGenType + "_" + appId;
            // 获取原始请求路径
            String resourcePath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            // 获取的资源文件后缀
            resourcePath = resourcePath.substring(("/app/view/" + appId).length());
            // 当路径为空时自动添加斜杠，避免路径解析问题
            if (resourcePath.isEmpty()) {
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", request.getRequestURI() + "/");
                return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
            }
            // 默认进入 index.html
            if (resourcePath.equals("/")) {
                resourcePath = "/index.html";
            }
            // 拼装完整的路径
            String filePath = CODE_OUTPUT_ROOT_DIR + "/" + viewKey + resourcePath;
            File file = new File(filePath.replace("/", File.separator));
            // 检查文件是否存在
            if (!file.exists()) {
                // 响应 notFound
                return ResponseEntity.notFound()
                        .build();
            }
            // 返回应用文件资源
            FileSystemResource fileSystemResource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header("Content-Type", getContentTypeWithCharset(filePath))
                    .body(fileSystemResource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 部署应用
     *
     * @param appId
     * @param loginUser
     * @return java.lang.String
     * @author DuRuiChi
     * @create 2025/8/27
     **/
    @Override
    public String deployApp(Long appId, User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 应用信息校验
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "未检测到应用");
        // 权限校验
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "这不是您的应用，无法部署");
        }
        // deployKey 校验
        String deployKey = app.getDeployKey();
        // 生成 deployKey
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(8);
        }
        // 复制文件（ 生成目录 到 部署目录 ）
        try {
            // 执行覆盖
            // 构建生成目录
            String codeGenType = app.getCodeGenType();
            String outputDirName = codeGenType + "_" + appId;
            File outputDir = new File(AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + outputDirName);
            if (!outputDir.exists() || !outputDir.isDirectory()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "未检测到源码，无法部署，请先生成源码");
            }
            // 构建部署目录
            File deployDir = new File(AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey);
            // 复制目录（执行覆盖）
            FileUtil.copyContent(outputDir, deployDir, true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用转储失败：" + e.getMessage());
        }
        // 更新应用信息
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "应用信息变更失败");
        // 部署 URL
        return String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
    }


    /**
     * 根据文件扩展名返回带字符编码的 Content-Type
     *
     * @param filePath 文件路径
     * @return java.lang.String 带字符编码的 Content-Type
     * @author DuRuiChi
     * @create 2025/8/10
     **/
    private String getContentTypeWithCharset(String filePath) {
        if (filePath.endsWith(".html")) return "text/html; charset=UTF-8";
        if (filePath.endsWith(".css")) return "text/css; charset=UTF-8";
        if (filePath.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (filePath.endsWith(".png")) return "image/png";
        if (filePath.endsWith(".jpg")) return "image/jpeg";
        return "application/octet-stream";
    }

    /**
     * 批量获取 AI 应用 VO 列表
     *
     * @param appList AI 应用列表
     * @return java.util.List<com.rich.richcodeweaver.model.vo.AppVO>
     * @author DuRuiChi
     * @create 2025/8/8
     **/
    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        // 空列表处理
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 提取所有用户ID（避免N+1查询）
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());

        // 批量查询用户信息并构建映射
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));

        // 转换 AI 应用实体为VO对象
        return appList.stream().map(app -> {
            // 获取基础VO信息
            AppVO appVO = getAppVO(app);
            // 设置关联用户信息
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    /**
     * 构造 AI 应用查询条件
     *
     * @param appQueryRequest AI 应用查询请求
     * @return com.mybatisflex.core.query.QueryWrapper  查询条件
     * @author DuRuiChi
     * @create 2025/8/8
     **/
    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        // 请求参数校验
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        // 解构请求参数
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();

        // 构建查询条件
        return QueryWrapper.create()
                // 精确匹配
                .eq("id", id)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                // 模糊查询
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                // 排序
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    /**
     * 添加 AI 应用
     *
     * @param appAddRequest AI 应用添加请求
     * @param request       请求
     * @return java.lang.Long
     * @author DuRuiChi
     * @create 2025/8/8
     **/
    @Override
    public Long addApp(AppAddRequest appAddRequest, HttpServletRequest request) {
        // 校验初始化提示
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "用户 prompt 不能为空");

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 创建 AI 应用实体
        App app = new App();
        // 复制请求属性到实体
        BeanUtil.copyProperties(appAddRequest, app);
        // 设置用户关联
        app.setUserId(loginUser.getId());
        // 生成 AI 应用名称（截取提示前 30 字符）
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 30)));
        // 设置默认生成策略
        // TODO 从请求参数获取
        app.setCodeGenType(CodeGeneratorTypeEnum.VUE_PROJECT.getValue());
        // 设置默认封面
        app.setCover(AppConstant.APP_COVER);

        // 保存 AI 应用数据
        boolean result = appService.save(app);
        // 校验保存结果
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return app.getId();
    }

    /**
     * 删除 AI 应用
     *
     * @param deleteRequest 删除请求
     * @param request       请求
     * @return java.lang.Boolean
     * @author DuRuiChi
     * @create 2025/8/8
     **/
    @Override
    public Boolean deleteApp(DeleteRequest deleteRequest, HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 解析 AI 应用ID
        long id = deleteRequest.getId();

        // 查询目标 AI 应用
        App oldApp = appService.getById(id);
        // 存在性校验
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);

        // 权限校验：所有者或管理员可删除
        if (!oldApp.getUserId().equals(loginUser.getId()) &&
                !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 删除关联历史对话消息记录
        ThrowUtils.throwIf(chatHistoryService.deleteByAppId(id), ErrorCode.OPERATION_ERROR);

        // 执行物理删除
        return appService.removeById(id);
    }

    /**
     * 更新 AI 应用
     *
     * @param appUpdateRequest AI 应用更新请求
     * @param request          请求
     * @return java.lang.Boolean
     * @author DuRuiChi
     * @create 2025/8/8
     **/
    @Override
    public Boolean updateApp(AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 解析 AI 应用ID
        long id = appUpdateRequest.getId();

        // 查询现有 AI 应用
        App oldApp = appService.getById(id);
        // 存在性校验
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);

        // 权限校验：仅所有者可更新
        if (!oldApp.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 创建更新对象
        App app = new App();
        // 设置更新ID
        app.setId(id);
        // 设置新 AI 应用名称
        app.setAppName(appUpdateRequest.getAppName());
        // 记录更新时间
        app.setEditTime(LocalDateTime.now());

        // 执行更新操作
        boolean result = appService.updateById(app);
        // 校验更新结果
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

    /**
     * 管理员更新 AI 应用
     *
     * @param appAdminUpdateRequest AI 应用管理员更新请求
     * @return java.lang.Boolean
     * @author DuRuiChi
     * @create 2025/8/8
     **/
    @Override
    public Boolean updateAppByAdmin(AppAdminUpdateRequest appAdminUpdateRequest) {
        // 解析 AI 应用ID
        long id = appAdminUpdateRequest.getId();

        // 查询现有 AI 应用
        App oldApp = appService.getById(id);
        // 存在性校验
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);

        // 复制管理员更新属性
        App app = new App();
        BeanUtil.copyProperties(appAdminUpdateRequest, app);
        // 记录更新时间
        app.setEditTime(LocalDateTime.now());

        // 执行管理员更新
        boolean result = appService.updateById(app);
        // 校验更新结果
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

    /**
     * 管理员获取 AI 应用分页
     *
     * @param appQueryRequest AI 应用查询请求
     * @return com.mybatisflex.core.paginate.Page<com.rich.richcodeweaver.model.vo.AppVO>
     * @author DuRuiChi
     * @create 2025/8/8
     **/
    @Override
    public Page<AppVO> listAppVOByPageByAdmin(AppQueryRequest appQueryRequest) {
        // 获取分页参数
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        // 构建查询条件
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        // 执行分页查询
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);

        // 构造VO分页
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        // 转换实体列表为VO列表
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return appVOPage;
    }

    /**
     * 获取我的 AI 应用分页
     *
     * @param appQueryRequest AI 应用查询请求
     * @param request         请求
     * @return com.mybatisflex.core.paginate.Page<com.rich.richcodeweaver.model.vo.AppVO>
     * @author DuRuiChi
     * @create 2025/8/8
     **/
    @Override
    public Page<AppVO> listMyAppVOByPage(AppQueryRequest appQueryRequest, HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 分页参数处理
        long pageSize = appQueryRequest.getPageSize();
        // 分页大小限制
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个 AI 应用");
        long pageNum = appQueryRequest.getPageNum();

        // 设置用户ID过滤条件
        appQueryRequest.setUserId(loginUser.getId());

        // 构建查询条件
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        // 执行分页查询
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);

        // 构造VO分页
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        // 转换实体列表为VO列表
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return appVOPage;
    }

    /**
     * 获取星标 AI 应用分页
     *
     * @param appQueryRequest AI 应用查询请求
     * @return com.mybatisflex.core.paginate.Page<com.rich.richcodeweaver.model.vo.AppVO>
     * @author DuRuiChi
     * @create 2025/8/8
     **/
    @Override
    public Page<AppVO> listStarAppVOByPage(AppQueryRequest appQueryRequest) {
        // 分页参数处理
        long pageSize = appQueryRequest.getPageSize();
        // 分页大小限制
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个 AI 应用");
        long pageNum = appQueryRequest.getPageNum();

        // 设置星标 AI 应用过滤条件
        appQueryRequest.setPriority(AppConstant.STAR_APP_PRIORITY);

        // 构建查询条件
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        // 执行分页查询
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);

        // 构造VO分页
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        // 转换实体列表为VO列表
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return appVOPage;
    }

    /**
     * 获取 AI 应用 VO 封装类
     *
     * @param app AI 应用实体类
     * @return com.rich.richcodeweaver.model.vo.AppVO
     * @author DuRuiChi
     * @create 2025/8/8
     **/
    @Override
    public AppVO getAppVO(App app) {
        // 空值校验
        if (app == null) {
            return null;
        }
        // 创建VO对象
        AppVO appVO = new AppVO();
        // 复制属性到VO对象
        BeanUtil.copyProperties(app, appVO);
        // 获取关联用户ID
        Long userId = app.getUserId();
        // 关联用户数据
        if (userId != null) {
            // 查询用户信息
            User user = userService.getById(userId);
            // 获取用户 VO 对象
            UserVO userVO = userService.getUserVO(user);
            // 设置用户 VO
            appVO.setUser(userVO);
        }
        return appVO;
    }
}
