package com.rich.client.innerService;

import java.util.List;

/**
 * 协作者服务内部接口
 * 通过Dubbo提供协作者关系的远程调用接口，供产物模块进行权限校验
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
public interface InnerCollaboratorService {

    /**
     * 判断用户是否为指定产物的协作者（已接受状态）
     *
     * @param appId  产物id
     * @param userId 用户id
     * @return 是否为协作者
     */
    boolean isCollaborator(Long appId, Long userId);

    /**
     * 获取指定产物所有已接受的协作者用户id列表
     *
     * @param appId 产物id
     * @return 协作者用户id列表
     */
    List<Long> listCollaboratorUserIds(Long appId);

    /**
     * 获取用户已接受的协作产物id列表
     *
     * @param userId 用户id
     * @return 协作产物id列表
     */
    List<Long> listCollaboratedAppIds(Long userId);
}
