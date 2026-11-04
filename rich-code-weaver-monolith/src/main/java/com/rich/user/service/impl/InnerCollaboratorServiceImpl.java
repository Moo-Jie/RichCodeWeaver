package com.rich.user.service.impl;

import com.rich.user.api.InnerCollaboratorService;
import com.rich.user.service.AppCollaboratorService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 协作者服务内部接口实现类
 * 作为本地 Spring Bean 提供内部服务，供产物模块进行权限校验
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@Service
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
