package com.rich.client.innerService;

import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.model.entity.User;
import com.rich.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import static com.rich.common.constant.UserConstant.ADMIN_ROLE;
import static com.rich.common.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务内部接口
 * 通过Dubbo提供用户服务的远程调用接口
 *
 * @author DuRuiChi
 * @since 2026-03-08
 */
public interface InnerUserService {
    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    static User getLoginUser(HttpServletRequest request) {
        // 从 session 获取登录态
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 判断用户是否为管理员
     *
     * @param user 用户对象
     * @return 是否为管理员
     */
    static boolean isAdmin(User user) {
        return user != null && ADMIN_ROLE.equals(user.getUserRole());
    }

    /**
     * 获取脱敏后的用户信息
     *
     * @param user 用户信息
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 通过 ID 集合获取用户列表
     *
     * @param ids
     * @return java.util.List<com.rich.richcodeweaver.model.entity.User>
     **/
    List<User> listByIds(Collection<? extends Serializable> ids);

    /**
     * 获取用户信息
     *
     * @param id
     * @return com.rich.richcodeweaver.model.entity.User
     **/
    User getById(Serializable id);
}
