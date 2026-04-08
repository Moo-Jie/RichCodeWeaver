package com.rich.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.model.entity.User;
import com.rich.model.entity.UserFriendship;
import com.rich.model.vo.UserFriendshipVO;
import com.rich.user.mapper.UserFriendshipMapper;
import com.rich.user.mapper.UserMapper;
import com.rich.user.service.UserFriendshipService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 好友关系 服务实现
 * 实现好友申请、处理、查询等核心业务逻辑
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@Service
public class UserFriendshipServiceImpl extends ServiceImpl<UserFriendshipMapper, UserFriendship>
        implements UserFriendshipService {

    @Resource
    private UserMapper userMapper;

    /**
     * 发送好友申请
     * 校验：不能添加自己、目标用户是否存在、是否已有申请记录
     *
     * @param userId   发起方用户id
     * @param friendId 目标用户id
     * @param remark   申请备注
     * @return 好友关系id
     */
    @Override
    public Long sendFriendRequest(Long userId, Long friendId, String remark) {
        // 不能添加自己为好友
        if (userId.equals(friendId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能添加自己为好友");
        }
        // 检查目标用户是否存在
        User friendUser = userMapper.selectOneById(friendId);
        if (friendUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        // 检查是否已经是好友
        if (isFriend(userId, friendId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已经是好友关系");
        }
        // 检查是否已有待处理的申请（双向检查）
        QueryWrapper pendingQuery = QueryWrapper.create()
                .from(UserFriendship.class)
                .where("((userId = ? AND friendId = ?) OR (userId = ? AND friendId = ?)) AND status = 0",
                        userId, friendId, friendId, userId);
        UserFriendship pendingRequest = getOne(pendingQuery);
        if (pendingRequest != null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已有待处理的好友申请");
        }
        // 检查是否存在已拒绝/已删除的旧记录（避免唯一键冲突）
        QueryWrapper oldQuery = QueryWrapper.create()
                .from(UserFriendship.class)
                .where("userId = ? AND friendId = ? AND status IN (2, 3)", userId, friendId);
        UserFriendship oldRecord = getOne(oldQuery);
        if (oldRecord != null) {
            // 重置为待处理状态
            oldRecord.setStatus(0);
            oldRecord.setRemark(remark);
            updateById(oldRecord);
            return oldRecord.getId();
        }
        // 创建好友申请
        UserFriendship friendship = UserFriendship.builder()
                .userId(userId)
                .friendId(friendId)
                .status(0)
                .remark(remark)
                .build();
        save(friendship);
        return friendship.getId();
    }

    /**
     * 处理好友申请（同意/拒绝）
     * 同意时会创建双向好友关系
     *
     * @param friendshipId 好友关系id
     * @param userId       当前用户id（必须是接收方）
     * @param action       处理动作: 1=同意, 2=拒绝
     * @return 操作是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleFriendRequest(Long friendshipId, Long userId, Integer action) {
        // 校验参数
        if (action != 1 && action != 2) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的处理动作");
        }
        // 查询申请记录
        UserFriendship friendship = getById(friendshipId);
        if (friendship == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "好友申请不存在");
        }
        // 必须是接收方才能处理
        if (!friendship.getFriendId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权处理该申请");
        }
        // 必须是待处理状态
        if (friendship.getStatus() != 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该申请已处理");
        }
        // 更新状态
        friendship.setStatus(action);
        updateById(friendship);

        // 如果同意，创建反向好友记录（形成双向关系）
        if (action == 1) {
            // 检查反向记录是否已存在
            QueryWrapper reverseQuery = QueryWrapper.create()
                    .from(UserFriendship.class)
                    .where("userId = ? AND friendId = ?", userId, friendship.getUserId());
            UserFriendship reverse = getOne(reverseQuery);
            if (reverse == null) {
                UserFriendship reverseFriendship = UserFriendship.builder()
                        .userId(userId)
                        .friendId(friendship.getUserId())
                        .status(1)
                        .remark("对方申请已同意")
                        .build();
                save(reverseFriendship);
            } else {
                reverse.setStatus(1);
                updateById(reverse);
            }
        }
        return true;
    }

    /**
     * 删除好友关系（双向删除）
     *
     * @param userId   当前用户id
     * @param friendId 好友用户id
     * @return 操作是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeFriend(Long userId, Long friendId) {
        // 删除双向好友记录
        QueryWrapper deleteQuery = QueryWrapper.create()
                .from(UserFriendship.class)
                .where("(userId = ? AND friendId = ?) OR (userId = ? AND friendId = ?)",
                        userId, friendId, friendId, userId);
        remove(deleteQuery);
        return true;
    }

    /**
     * 获取好友列表（已同意的），附带好友的用户信息
     *
     * @param userId 当前用户id
     * @return 好友列表VO
     */
    @Override
    public List<UserFriendshipVO> listFriends(Long userId) {
        // 查询当前用户作为发起方且状态为已同意的记录
        QueryWrapper query = QueryWrapper.create()
                .from(UserFriendship.class)
                .where("userId = ? AND status = 1", userId);
        List<UserFriendship> friendships = list(query);
        if (friendships.isEmpty()) {
            return new ArrayList<>();
        }
        // 批量获取好友用户信息
        Set<Long> friendIds = friendships.stream()
                .map(UserFriendship::getFriendId)
                .collect(Collectors.toSet());
        List<User> users = userMapper.selectListByIds(new ArrayList<>(friendIds));
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        // 组装VO
        return friendships.stream().map(f -> {
            UserFriendshipVO vo = new UserFriendshipVO();
            BeanUtil.copyProperties(f, vo);
            User friend = userMap.get(f.getFriendId());
            if (friend != null) {
                vo.setFriendName(friend.getUserName());
                vo.setFriendAvatar(friend.getUserAvatar());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 获取收到的待处理好友申请列表
     *
     * @param userId 当前用户id
     * @return 待处理的好友申请列表
     */
    @Override
    public List<UserFriendshipVO> listPendingRequests(Long userId) {
        // 查询当前用户作为接收方且状态为待处理的记录
        QueryWrapper query = QueryWrapper.create()
                .from(UserFriendship.class)
                .where("friendId = ? AND status = 0", userId)
                .orderBy("createTime", false);
        List<UserFriendship> friendships = list(query);
        if (friendships.isEmpty()) {
            return new ArrayList<>();
        }
        // 批量获取申请方用户信息
        Set<Long> requestUserIds = friendships.stream()
                .map(UserFriendship::getUserId)
                .collect(Collectors.toSet());
        List<User> users = userMapper.selectListByIds(new ArrayList<>(requestUserIds));
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        // 组装VO（这里friendName/friendAvatar代表申请方的信息）
        return friendships.stream().map(f -> {
            UserFriendshipVO vo = new UserFriendshipVO();
            BeanUtil.copyProperties(f, vo);
            User requester = userMap.get(f.getUserId());
            if (requester != null) {
                vo.setFriendName(requester.getUserName());
                vo.setFriendAvatar(requester.getUserAvatar());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 检查两个用户是否为好友（双向检查）
     *
     * @param userId   用户id
     * @param friendId 好友id
     * @return 是否为好友关系
     */
    @Override
    public boolean isFriend(Long userId, Long friendId) {
        QueryWrapper query = QueryWrapper.create()
                .from(UserFriendship.class)
                .where("userId = ? AND friendId = ? AND status = 1", userId, friendId);
        return count(query) > 0;
    }
}
