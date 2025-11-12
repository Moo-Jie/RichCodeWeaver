package com.rich.richcodeweaver.innerService;

import com.rich.richcodeweaver.exception.BusinessException;
import com.rich.richcodeweaver.exception.ErrorCode;
import com.rich.richcodeweaver.model.dto.user.UserUpdatePasswordRequest;
import com.rich.richcodeweaver.model.entity.User;
import com.rich.richcodeweaver.model.vo.LoginUserVO;
import com.rich.richcodeweaver.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import static com.rich.richcodeweaver.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务内部接口
 *
 * @author DuRuiChi
 * @create 2025/11/12
 **/
public interface InnerUserService {
    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    static User getLoginUser(HttpServletRequest request) {
        // 1. 从session获取登录态
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 2. 基础类型校验
        if (!(userObj instanceof User)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "登录状态异常");
        }

        // 3. 查询最新用户信息
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "登录状态异常");
        }
        return currentUser;
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
