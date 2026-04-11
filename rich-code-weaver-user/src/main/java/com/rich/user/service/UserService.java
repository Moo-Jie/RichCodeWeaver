package com.rich.user.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.rich.model.dto.user.UserQueryRequest;
import com.rich.model.dto.user.UserUpdatePasswordRequest;
import com.rich.model.entity.User;
import com.rich.model.vo.LoginUserVO;
import com.rich.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户 服务层
 * 提供用户注册、登录、信息管理等业务逻辑
 *
 * @author DuRuiChi
 * @since 2026-03-08
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册（邮箱方式）
     *
     * @param email         邮箱
     * @param userName      用户昵称
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String email, String userName, String userPassword, String checkPassword);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 用户登录（邮箱方式）
     *
     * @param email        邮箱
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String email, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取脱敏后的用户信息
     *
     * @param user 用户信息
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户信息（分页）
     *
     * @param userList 用户列表
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 用户注销
     *
     * @param request
     * @return 退出登录是否成功
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 根据查询条件构造数据查询参数
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 加密
     *
     * @param userPassword 用户密码
     * @return 加密后的用户密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 更新密码
     *
     * @param userUpdatePasswordRequest
     * @return
     */
    Boolean updatePassword(UserUpdatePasswordRequest userUpdatePasswordRequest);

    /**
     * 重置密码
     *
     * @param userId 用户 ID
     * @return 重置密码是否成功
     */
    Boolean resetPassword(long userId);

    /**
     * 根据用户名模糊搜索用户（排除指定用户）
     *
     * @param keyword       搜索关键词
     * @param excludeUserId 需要排除的用户id（通常为当前登录用户）
     * @return 匹配的用户VO列表（最多20条）
     */
    List<UserVO> searchUsersByName(String keyword, Long excludeUserId);
}
