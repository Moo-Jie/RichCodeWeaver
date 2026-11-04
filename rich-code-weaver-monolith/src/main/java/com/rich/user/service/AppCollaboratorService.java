package com.rich.user.service;

import com.mybatisflex.core.service.IService;
import com.rich.model.entity.AppCollaborator;
import com.rich.model.vo.AppCollaboratorVO;

import java.util.List;

/**
 * 产物协作者 服务接口
 * 提供协作者的邀请、处理、查询、移除等业务逻辑
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
public interface AppCollaboratorService extends IService<AppCollaborator> {

    /**
     * 邀请好友成为产物协作者
     * 校验：产物归属、好友关系、身份非个人、重复邀请、不能邀请自己
     *
     * @param appId     产物id
     * @param userId    被邀请用户id
     * @param inviterId 邀请人用户id（产物所有者）
     * @param role      协作角色
     * @return 协作关系id
     */
    Long inviteCollaborator(Long appId, Long userId, Long inviterId, String role);

    /**
     * 处理协作邀请（接受/拒绝）
     *
     * @param id     协作关系id
     * @param userId 当前用户id（被邀请者）
     * @param action 1=接受, 2=拒绝
     * @return 是否处理成功
     */
    boolean handleInvitation(Long id, Long userId, int action);

    /**
     * 移除协作者
     *
     * @param appId     产物id
     * @param userId    被移除的协作者用户id
     * @param operatorId 操作人用户id（产物所有者）
     * @return 是否移除成功
     */
    boolean removeCollaborator(Long appId, Long userId, Long operatorId);

    /**
     * 获取指定产物的已接受协作者列表（包含用户信息）
     *
     * @param appId 产物id
     * @return 协作者视图对象列表
     */
    List<AppCollaboratorVO> listCollaborators(Long appId);

    /**
     * 获取当前用户收到的待处理协作邀请列表
     *
     * @param userId 当前用户id
     * @return 协作邀请视图对象列表
     */
    List<AppCollaboratorVO> listPendingInvitations(Long userId);

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
     * 获取用户参与协作的产物id列表（已接受状态）
     *
     * @param userId 用户id
     * @return 产物id列表
     */
    List<Long> listCollaboratedAppIds(Long userId);
}
