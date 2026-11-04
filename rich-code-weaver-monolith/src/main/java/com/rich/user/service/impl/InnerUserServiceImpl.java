package com.rich.user.service.impl;

import com.rich.user.api.InnerUserService;
import com.rich.model.entity.User;
import com.rich.model.vo.UserVO;
import com.rich.user.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 提供本地用户内部服务的实现类
 * 作为本地 Spring Bean 提供内部服务
 *
 * @author DuRuiChi
 * @since 2026-03-08
 */
@Service
public class InnerUserServiceImpl implements InnerUserService {
    /**
     * 调用模块内部的实现类
     */
    @Resource
    private UserService userService;

    @Override
    public UserVO getUserVO(User user) {
        return userService.getUserVO(user);
    }

    @Override
    public List<User> listByIds(Collection<? extends Serializable> ids) {
        return userService.listByIds(ids);
    }

    @Override
    public User getById(Serializable id) {
        return userService.getById(id);
    }
}
