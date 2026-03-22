package com.rich.user.service.impl;

import com.rich.client.innerService.InnerUserService;
import com.rich.model.entity.User;
import com.rich.model.vo.UserVO;
import com.rich.user.service.UserService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 提供外部调用用户服务的实现类
 * 通过Dubbo提供服务发现和远程调用能力
 *
 * @author DuRuiChi
 * @since 2026-03-08
 */
@DubboService
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
