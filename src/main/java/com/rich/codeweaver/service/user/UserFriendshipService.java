package com.rich.codeweaver.service.user;

import com.mybatisflex.core.service.IService;
import com.rich.codeweaver.model.entity.UserFriendship;
import com.rich.codeweaver.model.vo.UserFriendshipVO;

import java.util.List;

/**
 * 好友关系 服务层
 * 提供好友申请、处理、查询等业务逻辑
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
public interface UserFriendshipService extends IService<UserFriendship> {

    /**
     * 发送好友申请
     *
     * @param userId   发起方用户id
     * @param friendId 目标用户id
     * @param remark   申请备注
     * @return 好友关系id
     */
    Long sendFriendRequest(Long userId, Long friendId, String remark);

    /**
     * 处理好友申请（同意/拒绝）
     *
     * @param friendshipId 好友关系id
     * @param userId       当前用户id（必须是接收方）
     * @param action       处理动作: 1=同意, 2=拒绝
     * @return 操作是否成功
     */
    boolean handleFriendRequest(Long friendshipId, Long userId, Integer action);

    /**
     * 删除好友关系
     *
     * @param userId   当前用户id
     * @param friendId 好友用户id
     * @return 操作是否成功
     */
    boolean removeFriend(Long userId, Long friendId);

    /**
     * 获取好友列表（已同意的）
     *
     * @param userId 当前用户id
     * @return 好友列表VO
     */
    List<UserFriendshipVO> listFriends(Long userId);

    /**
     * 获取收到的好友申请列表（待处理）
     *
     * @param userId 当前用户id
     * @return 待处理的好友申请列表
     */
    List<UserFriendshipVO> listPendingRequests(Long userId);

    /**
     * 检查两个用户是否为好友
     *
     * @param userId   用户id
     * @param friendId 好友id
     * @return 是否为好友关系
     */
    boolean isFriend(Long userId, Long friendId);
}
