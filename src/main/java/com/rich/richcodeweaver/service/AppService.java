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
import com.rich.richcodeweaver.model.vo.AppVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 应用 服务层。
 *
 *
 */
public interface AppService extends IService<App> {

    /**
     * 获取应用封装类
     *
     * @param app
     * @return
     */
    AppVO getAppVO(App app);

    /**
     * 获取应用封装类列表
     *
     * @param appList
     * @return
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 构造应用查询条件
     *
     * @param appQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 添加应用
     *
     * @param appAddRequest 应用添加请求
     * @param request       请求
     * @return  应用id
     */
    Long addApp(AppAddRequest appAddRequest, HttpServletRequest request);

    /**
     * 更新应用
     *
     * @param appUpdateRequest 应用更新请求
     * @return 是否更新成功
     */
    Boolean updateApp(AppUpdateRequest appUpdateRequest, HttpServletRequest request);

    /**
     * 删除应用
     *
     * @param deleteRequest 删除请求
     * @param request       请求
     * @return 是否删除成功
     */
    Boolean deleteApp(DeleteRequest deleteRequest, HttpServletRequest request);

    /**
     * 分页获取我的应用列表
     *
     * @param appQueryRequest 应用查询请求
     * @param request         请求
     * @return 应用视图对象分页
     */
    Page<AppVO> listMyAppVOByPage(AppQueryRequest appQueryRequest, HttpServletRequest request);

    /**
     * 分页获取星标应用列表
     *
     * @param appQueryRequest 应用查询请求
     * @return 应用视图对象分页
     */
    Page<AppVO> listStarAppVOByPage(AppQueryRequest appQueryRequest);

    /**
     * 管理员更新应用
     *
     * @param appAdminUpdateRequest 应用管理员更新请求
     * @return 是否更新成功
     */
    Boolean updateAppByAdmin(AppAdminUpdateRequest appAdminUpdateRequest);

    /**
     * 管理员分页获取应用列表
     *
     * @param appQueryRequest 应用查询请求
     * @return 应用视图对象分页
     */
    Page<AppVO> listAppVOByPageByAdmin(AppQueryRequest appQueryRequest);
}
