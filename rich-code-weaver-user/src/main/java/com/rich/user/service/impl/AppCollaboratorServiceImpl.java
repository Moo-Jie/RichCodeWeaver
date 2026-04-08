package com.rich.user.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.model.entity.AppCollaborator;
import com.rich.model.entity.User;
import com.rich.model.vo.AppCollaboratorVO;
import com.rich.user.mapper.AppCollaboratorMapper;
import com.rich.user.mapper.UserMapper;
import com.rich.user.service.AppCollaboratorService;
import com.rich.user.service.UserFriendshipService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 产物协作者 服务实现
 * 实现协作者邀请、处理、查询、移除等核心业务逻辑
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@Slf4j
@Service
public class AppCollaboratorServiceImpl extends ServiceImpl<AppCollaboratorMapper, AppCollaborator>
        implements AppCollaboratorService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private AppCollaboratorMapper collaboratorMapper;

    @Resource
    private UserFriendshipService userFriendshipService;

    /** 协作状态常量 */
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_ACCEPTED = 1;
    private static final int STATUS_REJECTED = 2;
    private static final int STATUS_REMOVED = 3;

    /** 单个产物最大协作者数量 */
    private static final int MAX_COLLABORATORS = 20;

    /**
     * 邀请好友成为产物协作者
     * 校验：不能邀请自己、好友关系、重复邀请、协作者数量上限
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long inviteCollaborator(Long appId, Long userId, Long inviterId, String role) {
        Map<String, Object> appInfo = collaboratorMapper.selectAppInfoById(appId);
        if (appInfo == null || appInfo.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "产物不存在");
        }
        Object ownerIdObj = appInfo.get("userId");
        Long ownerId = ownerIdObj instanceof Number ? ((Number) ownerIdObj).longValue() : null;
        if (ownerId == null || !ownerId.equals(inviterId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅产物所有者可邀请协作者");
        }

        // 不能邀请自己
        if (inviterId.equals(userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能邀请自己为协作者");
        }

        // 校验被邀请用户存在
        User targetUser = userMapper.selectOneById(userId);
        if (targetUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "被邀请的用户不存在");
        }

        // 校验好友关系
        if (!userFriendshipService.isFriend(inviterId, userId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只能邀请好友成为协作者");
        }

        // 检查是否已有协作记录（含待确认、已接受状态）
        QueryWrapper existQuery = QueryWrapper.create()
                .from(AppCollaborator.class)
                .where("appId = ? AND userId = ? AND status IN (0, 1)", appId, userId);
        AppCollaborator existing = getOne(existQuery);
        if (existing != null) {
            if (existing.getStatus() == STATUS_ACCEPTED) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "该用户已是协作者");
            }
            if (existing.getStatus() == STATUS_PENDING) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "已向该用户发送过邀请，请等待确认");
            }
        }

        // 检查协作者数量上限
        long count = count(QueryWrapper.create()
                .from(AppCollaborator.class)
                .where("appId = ? AND status = ?", appId, STATUS_ACCEPTED));
        if (count >= MAX_COLLABORATORS) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "协作者数量已达上限（" + MAX_COLLABORATORS + "人）");
        }

        // 如果存在已拒绝/已移除的旧记录，更新为待确认
        QueryWrapper oldQuery = QueryWrapper.create()
                .from(AppCollaborator.class)
                .where("appId = ? AND userId = ? AND status IN (2, 3)", appId, userId);
        AppCollaborator oldRecord = getOne(oldQuery);
        if (oldRecord != null) {
            oldRecord.setStatus(STATUS_PENDING);
            oldRecord.setInviterId(inviterId);
            oldRecord.setRole(role != null ? role : "editor");
            updateById(oldRecord);
            log.info("重新邀请协作者: appId={}, userId={}, inviterId={}", appId, userId, inviterId);
            return oldRecord.getId();
        }

        // 创建新的协作关系记录
        AppCollaborator collaborator = AppCollaborator.builder()
                .appId(appId)
                .userId(userId)
                .inviterId(inviterId)
                .status(STATUS_PENDING)
                .role(role != null ? role : "editor")
                .build();
        save(collaborator);
        log.info("邀请协作者: appId={}, userId={}, inviterId={}, id={}", appId, userId, inviterId, collaborator.getId());
        return collaborator.getId();
    }

    /**
     * 处理协作邀请（接受/拒绝）
     * 校验：记录存在、当前用户为被邀请者、状态为待确认
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleInvitation(Long id, Long userId, int action) {
        AppCollaborator collaborator = getById(id);
        if (collaborator == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "协作邀请不存在");
        }
        // 必须是被邀请者本人处理
        if (!collaborator.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权处理该邀请");
        }
        // 必须是待确认状态
        if (collaborator.getStatus() != STATUS_PENDING) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该邀请已被处理");
        }

        if (action == 1) {
            long count = count(QueryWrapper.create()
                    .from(AppCollaborator.class)
                    .where("appId = ? AND status = ?", collaborator.getAppId(), STATUS_ACCEPTED));
            if (count >= MAX_COLLABORATORS) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "协作者数量已达上限（" + MAX_COLLABORATORS + "人）");
            }
            collaborator.setStatus(STATUS_ACCEPTED);
        } else if (action == 2) {
            collaborator.setStatus(STATUS_REJECTED);
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的处理动作");
        }

        boolean result = updateById(collaborator);
        log.info("处理协作邀请: id={}, userId={}, action={}, result={}", id, userId, action, result);
        return result;
    }

    /**
     * 移除协作者（仅产物所有者可操作）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeCollaborator(Long appId, Long userId, Long operatorId) {
        QueryWrapper query = QueryWrapper.create()
                .from(AppCollaborator.class)
                .where("appId = ? AND userId = ? AND status = ?", appId, userId, STATUS_ACCEPTED);
        AppCollaborator collaborator = getOne(query);
        if (collaborator == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "协作关系不存在");
        }
        // 校验操作者是邀请人（产物所有者）
        if (!collaborator.getInviterId().equals(operatorId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅产物所有者可移除协作者");
        }
        collaborator.setStatus(STATUS_REMOVED);
        boolean result = updateById(collaborator);
        log.info("移除协作者: appId={}, userId={}, operatorId={}", appId, userId, operatorId);
        return result;
    }

    /**
     * 获取指定产物的已接受协作者列表（包含用户信息）
     */
    @Override
    public List<AppCollaboratorVO> listCollaborators(Long appId) {
        QueryWrapper query = QueryWrapper.create()
                .from(AppCollaborator.class)
                .where("appId = ? AND status = ?", appId, STATUS_ACCEPTED)
                .orderBy("createTime", true);
        List<AppCollaborator> records = list(query);
        return enrichCollaboratorVOs(records);
    }

    /**
     * 获取当前用户收到的待处理协作邀请列表
     */
    @Override
    public List<AppCollaboratorVO> listPendingInvitations(Long userId) {
        QueryWrapper query = QueryWrapper.create()
                .from(AppCollaborator.class)
                .where("userId = ? AND status = ?", userId, STATUS_PENDING)
                .orderBy("createTime", false);
        List<AppCollaborator> records = list(query);
        return enrichCollaboratorVOs(records);
    }

    /**
     * 判断用户是否为指定产物的协作者（已接受状态）
     */
    @Override
    public boolean isCollaborator(Long appId, Long userId) {
        if (appId == null || userId == null) {
            return false;
        }
        QueryWrapper query = QueryWrapper.create()
                .from(AppCollaborator.class)
                .where("appId = ? AND userId = ? AND status = ?", appId, userId, STATUS_ACCEPTED);
        return count(query) > 0;
    }

    /**
     * 获取指定产物所有已接受的协作者用户id列表
     */
    @Override
    public List<Long> listCollaboratorUserIds(Long appId) {
        QueryWrapper query = QueryWrapper.create()
                .from(AppCollaborator.class)
                .where("appId = ? AND status = ?", appId, STATUS_ACCEPTED);
        List<AppCollaborator> records = list(query);
        return records.stream().map(AppCollaborator::getUserId).collect(Collectors.toList());
    }

    /**
     * 获取用户参与协作的产物id列表（已接受状态）
     */
    @Override
    public List<Long> listCollaboratedAppIds(Long userId) {
        QueryWrapper query = QueryWrapper.create()
                .from(AppCollaborator.class)
                .where("userId = ? AND status = ?", userId, STATUS_ACCEPTED);
        List<AppCollaborator> records = list(query);
        return records.stream().map(AppCollaborator::getAppId).collect(Collectors.toList());
    }

    // ===== 内部工具方法 =====

    /**
     * 为协作者记录列表填充用户信息，转换为VO
     *
     * @param records 协作者实体列表
     * @return 协作者视图对象列表
     */
    private List<AppCollaboratorVO> enrichCollaboratorVOs(List<AppCollaborator> records) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }

        // 收集所有需要查询的用户id（协作者id + 邀请人id）
        Set<Long> userIds = records.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getUserId(), r.getInviterId()))
                .collect(Collectors.toSet());
        // 批量查询用户信息
        List<User> users = userMapper.selectListByIds(new ArrayList<>(userIds));
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        // 收集所有需要查询的产物id
        Set<Long> appIds = records.stream()
                .map(AppCollaborator::getAppId)
                .collect(Collectors.toSet());
        // 批量查询产物信息（名称和封面）
        Map<Long, Map<String, Object>> appMap = new java.util.HashMap<>();
        if (!appIds.isEmpty()) {
            List<Map<String, Object>> appInfos = collaboratorMapper.selectAppInfoByIds(appIds);
            for (Map<String, Object> info : appInfos) {
                Object idObj = info.get("id");
                if (idObj != null) {
                    Long appId = ((Number) idObj).longValue();
                    appMap.put(appId, info);
                }
            }
        }

        // 组装VO列表
        List<AppCollaboratorVO> voList = new ArrayList<>();
        for (AppCollaborator record : records) {
            AppCollaboratorVO vo = new AppCollaboratorVO();
            vo.setId(record.getId());
            vo.setAppId(record.getAppId());
            vo.setUserId(record.getUserId());
            vo.setInviterId(record.getInviterId());
            vo.setStatus(record.getStatus());
            vo.setRole(record.getRole());
            vo.setCreateTime(record.getCreateTime());
            vo.setUpdateTime(record.getUpdateTime());

            // 填充协作者用户信息
            User collabUser = userMap.get(record.getUserId());
            if (collabUser != null) {
                vo.setUserName(collabUser.getUserName());
                vo.setUserAvatar(collabUser.getUserAvatar());
            }

            // 填充邀请人用户信息
            User inviter = userMap.get(record.getInviterId());
            if (inviter != null) {
                vo.setInviterName(inviter.getUserName());
            }

            // 填充产物信息
            Map<String, Object> appInfo = appMap.get(record.getAppId());
            if (appInfo != null) {
                vo.setAppName((String) appInfo.get("appName"));
                vo.setAppCover((String) appInfo.get("cover"));
            }

            voList.add(vo);
        }
        return voList;
    }
}
