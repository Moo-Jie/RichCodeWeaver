package com.rich.user.service.impl;

import com.rich.client.innerService.InnerCollaboratorService;
import com.rich.user.service.AppCollaboratorService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

/**
 * 协作者服务内部接口实现类
 * 通过Dubbo提供服务发现和远程调用能力，供产物模块进行权限校验
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@DubboService
public class InnerCollaboratorServiceImpl implements InnerCollaboratorService {

    @Resource
    private AppCollaboratorService appCollaboratorService;

    @Override
    public boolean isCollaborator(Long appId, Long userId) {
        return appCollaboratorService.isCollaborator(appId, userId);
    }

    @Override
    public List<Long> listCollaboratorUserIds(Long appId) {
        return appCollaboratorService.listCollaboratorUserIds(appId);
    }

    @Override
    public List<Long> listCollaboratedAppIds(Long userId) {
        return appCollaboratorService.listCollaboratedAppIds(userId);
    }
}
