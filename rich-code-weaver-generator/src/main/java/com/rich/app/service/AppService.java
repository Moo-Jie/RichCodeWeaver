package com.rich.app.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.rich.common.model.DeleteRequest;
import com.rich.model.dto.app.AppAddRequest;
import com.rich.model.dto.app.AppAdminUpdateRequest;
import com.rich.model.dto.app.AppQueryRequest;
import com.rich.model.dto.app.AppUpdateRequest;
import com.rich.model.entity.App;
import com.rich.model.entity.User;
import com.rich.model.vo.AppVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

/**
 * AI 产物 服务层
 * 提供AI产物的业务逻辑处理，包括代码生成、部署、预览等功能
 *
 * @author DuRuiChi
 * @since 2026-03-10
 */
public interface AppService extends IService<App> {

    /**
     * 获取 AI 产物封装类
     *
     * @param app 产物实体
     * @return com.rich.model.vo.AppVO 产物视图对象
     * @author DuRuiChi
     */
    AppVO getAppVO(App app);

    /**
     * 获取 AI 产物封装类列表
     *
     * @param appList 产物实体列表
     * @return java.util.List<com.rich.model.vo.AppVO> 产物视图对象列表
     * @author DuRuiChi
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 构造 AI 产物查询条件
     *
     * @param appQueryRequest 查询请求参数
     * @return com.mybatisflex.core.query.QueryWrapper 查询包装类
     * @author DuRuiChi
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 添加 AI 产物
     *
     * @param appAddRequest AI 产物添加请求
     * @param request       请求对象
     * @return java.lang.Long 新创建的产物ID
     * @author DuRuiChi
     */
    Long addApp(AppAddRequest appAddRequest, HttpServletRequest request);

    /**
     * 更新 AI 产物
     *
     * @param appUpdateRequest AI 产物更新请求
     * @param request          请求对象
     * @return java.lang.Boolean 是否更新成功
     * @author DuRuiChi
     */
    Boolean updateApp(AppUpdateRequest appUpdateRequest, HttpServletRequest request);

    /**
     * 删除 AI 产物
     *
     * @param deleteRequest 删除请求
     * @param request       请求对象
     * @return java.lang.Boolean 是否删除成功
     * @author DuRuiChi
     */
    Boolean deleteApp(DeleteRequest deleteRequest, HttpServletRequest request);

    /**
     * 分页获取我的 AI 产物列表
     *
     * @param appQueryRequest AI 产物查询请求
     * @param request         请求对象
     * @return com.mybatisflex.core.paginate.Page<com.rich.model.vo.AppVO> 产物视图对象分页
     * @author DuRuiChi
     */
    Page<AppVO> listMyAppVOByPage(AppQueryRequest appQueryRequest, HttpServletRequest request);

    /**
     * 分页获取星标 AI 产物列表
     *
     * @param appQueryRequest AI 产物查询请求
     * @return com.mybatisflex.core.paginate.Page<com.rich.model.vo.AppVO> 产物视图对象分页
     * @author DuRuiChi
     */
    Page<AppVO> listStarAppVOByPage(AppQueryRequest appQueryRequest);

    /**
     * 获取用户相关的产物列表
     *
     * @param userId 用户ID
     * @param limit  限制条数
     * @return java.util.List<com.rich.model.vo.AppVO> 产物视图对象列表
     * @author DuRuiChi
     */
    List<AppVO> listUserRelatedApps(Long userId, long limit);

    /**
     * 管理员更新 AI 产物
     *
     * @param appAdminUpdateRequest AI 产物管理员更新请求
     * @return java.lang.Boolean 是否更新成功
     * @author DuRuiChi
     */
    Boolean updateAppByAdmin(AppAdminUpdateRequest appAdminUpdateRequest);

    /**
     * 管理员分页获取 AI 产物列表
     *
     * @param appQueryRequest AI 产物查询请求
     * @return com.mybatisflex.core.paginate.Page<com.rich.model.vo.AppVO> 产物视图对象分页
     * @author DuRuiChi
     */
    Page<AppVO> listAppVOByPageByAdmin(AppQueryRequest appQueryRequest);

    /**
     * 执行 AI 对话并生成代码(流式，支持断线重连)
     * 使用工作流分布执行节点模式
     *
     * @param appId       AI 产物id
     * @param userId      用户id
     * @param message     对话消息
     * @param materialIds 选中的素材ID列表（可选）
     * @param isWorkflow  是否开启 Agent 模式（前端参数，暂时保留用于未来 Agent 模式）
     * @param lastEventId 最后接收到的事件ID（用于断线重连）
     * @param reconnect   是否为重连请求
     * @return reactor.core.publisher.Flux<org.springframework.http.codec.ServerSentEvent < java.lang.String>> 代码生成事件流
     * @author DuRuiChi
     */
    Flux<ServerSentEvent<String>> aiChatAndGenerateCodeStreamWithReconnect(Long appId, Long userId, String message, List<Long> materialIds, Boolean isWorkflow, String lastEventId, Boolean reconnect);

    /**
     * 预览指定产物
     *
     * @param appId   产物 ID
     * @param request 请求对象
     * @return org.springframework.http.ResponseEntity<org.springframework.core.io.FileSystemResource> 产物资源响应
     * @author DuRuiChi
     */
    ResponseEntity<FileSystemResource> serverStaticResource(Long appId, HttpServletRequest request);

    /**
     * 部署产物
     *
     * @param appId     产物id
     * @param loginUser 登录用户
     * @return java.lang.String 部署后的产物访问URL
     * @author DuRuiChi
     */
    String deployApp(Long appId, User loginUser);

    /**
     * 构建代码输出文件夹
     *
     * @param app 产物实体
     * @return java.io.File 代码输出目录
     * @author DuRuiChi
     **/
    File getOutputDir(App app);

    /**
     * 构建代码部署文件夹
     *
     * @param deployKey 部署密钥
     * @return java.io.File 代码部署目录
     * @author DuRuiChi
     **/
    File getDeployDir(String deployKey);

    /**
     * 构建用于删除的代码输出文件夹（没有 dist 直接删除整个目录）
     *
     * @param app 产物实体
     * @return java.io.File 待删除的代码输出目录
     * @author DuRuiChi
     **/
    File getDelOutputDir(App app);
}
