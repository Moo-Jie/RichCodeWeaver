package com.rich.richcodeweaver.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.rich.richcodeweaver.model.common.DeleteRequest;
import com.rich.richcodeweaver.model.dto.app.AppAddRequest;
import com.rich.richcodeweaver.model.dto.app.AppAdminUpdateRequest;
import com.rich.richcodeweaver.model.dto.app.AppQueryRequest;
import com.rich.richcodeweaver.model.dto.app.AppUpdateRequest;
import com.rich.richcodeweaver.model.entity.App;
import com.rich.richcodeweaver.model.entity.User;
import com.rich.richcodeweaver.model.vo.AppVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

/**
 * AI 应用 服务层。
 */
public interface AppService extends IService<App> {

    /**
     * 获取 AI 应用封装类
     *
     * @param app
     * @return
     */
    AppVO getAppVO(App app);

    /**
     * 获取 AI 应用封装类列表
     *
     * @param appList
     * @return
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 构造 AI 应用查询条件
     *
     * @param appQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 添加 AI 应用
     *
     * @param appAddRequest AI 应用添加请求
     * @param request       请求
     * @return AI 应用id
     */
    Long addApp(AppAddRequest appAddRequest, HttpServletRequest request);

    /**
     * 更新 AI 应用
     *
     * @param appUpdateRequest AI 应用更新请求
     * @return 是否更新成功
     */
    Boolean updateApp(AppUpdateRequest appUpdateRequest, HttpServletRequest request);

    /**
     * 删除 AI 应用
     *
     * @param deleteRequest 删除请求
     * @param request       请求
     * @return 是否删除成功
     */
    Boolean deleteApp(DeleteRequest deleteRequest, HttpServletRequest request);

    /**
     * 分页获取我的 AI 应用列表
     *
     * @param appQueryRequest AI 应用查询请求
     * @param request         请求
     * @return AI 应用视图对象分页
     */
    Page<AppVO> listMyAppVOByPage(AppQueryRequest appQueryRequest, HttpServletRequest request);

    /**
     * 分页获取星标 AI 应用列表
     *
     * @param appQueryRequest AI 应用查询请求
     * @return AI 应用视图对象分页
     */
    Page<AppVO> listStarAppVOByPage(AppQueryRequest appQueryRequest);

    /**
     * 管理员更新 AI 应用
     *
     * @param appAdminUpdateRequest AI 应用管理员更新请求
     * @return 是否更新成功
     */
    Boolean updateAppByAdmin(AppAdminUpdateRequest appAdminUpdateRequest);

    /**
     * 管理员分页获取 AI 应用列表
     *
     * @param appQueryRequest AI 应用查询请求
     * @return AI 应用视图对象分页
     */
    Page<AppVO> listAppVOByPageByAdmin(AppQueryRequest appQueryRequest);

    /**
     * 执行 AI 对话并并生成代码(流式)
     *
     * @param appId   AI 应用id
     * @param userId  用户id
     * @param message 对话消息
     * @return 代码流
     */
    Flux<ServerSentEvent<String>> aiChatAndGenerateCodeStream(Long appId, Long userId, String message);

    /**
     * 执行 AI 对话并并生成代码(非流式)
     *
     * @param appId   AI 应用id
     * @param userId  用户id
     * @param message 对话消息
     * @return 代码文件
     */
    File aiChatAndGenerateCode(Long appId, Long userId, String message);

    /**
     * 预览指定应用
     *
     * @param appId   应用 ID
     * @param request 请求对象
     * @return org.springframework.http.ResponseEntity<jakarta.annotation.Resource> 应用资源
     */
    ResponseEntity<FileSystemResource> serverStaticResource(Long appId, HttpServletRequest request);

    /**
     * 部署应用
     *
     * @param appId     应用id
     * @param loginUser 登录用户
     * @return 部署后的应用访问url
     */
    String deployApp(Long appId, User loginUser);

    /**
     * 构建代码输出文件夹
     *
     * @param app 应用
     * @return java.io.File
     **/
    public File getOutputDir(App app);

    /**
     * 构建代码部署文件夹
     *
     * @param deployKey 部署密钥
     * @return java.io.File
     **/
    public File getDeployDir(String deployKey);

    /**
     * 构建用于删除的代码输出文件夹（没有 dist 直接删除整个目录）
     *
     * @param app 应用
     * @return java.io.File
     **/
    public File getDelOutputDir(App app);
}
