package com.rich.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.model.entity.AppCollaborator;
import com.rich.model.entity.ChatConversation;
import com.rich.model.entity.ChatMessage;
import com.rich.model.entity.User;
import com.rich.model.vo.ChatMessageVO;
import com.rich.user.mapper.AppCollaboratorMapper;
import com.rich.user.mapper.ChatMessageMapper;
import com.rich.user.mapper.UserMapper;
import com.rich.user.service.ChatConversationService;
import com.rich.user.service.ChatMessageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 聊天消息 服务实现
 * 实现消息发送、查询、已读标记等核心业务逻辑
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
        implements ChatMessageService {

    @Resource
    private ChatConversationService chatConversationService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private AppCollaboratorMapper appCollaboratorMapper;

    /**
     * 发送消息
     * 自动获取或创建会话，保存消息并更新会话的最后消息信息
     *
     * @param senderId    发送方用户id
     * @param receiverId  接收方用户id
     * @param content     消息内容
     * @param messageType 消息类型
     * @return 消息VO（包含发送方用户信息）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO sendMessage(Long senderId, Long receiverId, String content, String messageType) {
        // 参数校验
        if (senderId.equals(receiverId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能给自己发消息");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        }
        if (messageType == null || messageType.trim().isEmpty()) {
            messageType = "text";
        }

        // 获取或创建会话
        ChatConversation conversation = chatConversationService.getOrCreateConversation(senderId, receiverId);

        // 创建消息
        ChatMessage message = ChatMessage.builder()
                .conversationId(conversation.getId())
                .senderId(senderId)
                .receiverId(receiverId)
                .content(content.trim())
                .messageType(messageType)
                .isRead(0)
                .build();
        save(message);

        // 更新会话最后消息
        chatConversationService.updateLastMessage(conversation.getId(), message.getId());

        // 组装VO返回
        User sender = userMapper.selectOneById(senderId);
        return buildMessageVO(message, sender, loadCollaboratorMap(List.of(message)));
    }

    /**
     * 分页查询会话消息（按时间升序）
     *
     * @param conversationId 会话id
     * @param currentUserId  当前登录用户id
     * @param pageNum        页码
     * @param pageSize       每页数量
     * @return 消息分页（含发送方用户信息）
     */
    @Override
    public Page<ChatMessageVO> listMessages(Long conversationId, Long currentUserId, long pageNum, long pageSize) {
        validateConversationAccess(conversationId, currentUserId);
        // 查询消息（按时间升序）
        QueryWrapper query = QueryWrapper.create()
                .from(ChatMessage.class)
                .where("conversationId = ?", conversationId)
                .orderBy("createTime", true);
        Page<ChatMessage> messagePage = page(Page.of(pageNum, pageSize), query);

        // 批量获取发送方用户信息
        Set<Long> senderIds = messagePage.getRecords().stream()
                .map(ChatMessage::getSenderId)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = Map.of();
        if (!senderIds.isEmpty()) {
            List<User> users = userMapper.selectListByIds(new ArrayList<>(senderIds));
            userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        }

        Map<Long, AppCollaborator> collaboratorMap = loadCollaboratorMap(messagePage.getRecords());

        // 组装VO分页
        Map<Long, User> finalUserMap = userMap;
        Map<Long, AppCollaborator> finalCollaboratorMap = collaboratorMap;
        Page<ChatMessageVO> voPage = new Page<>(pageNum, pageSize, messagePage.getTotalRow());
        List<ChatMessageVO> voList = messagePage.getRecords().stream().map(m -> {
            return buildMessageVO(m, finalUserMap.get(m.getSenderId()), finalCollaboratorMap);
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 标记会话中接收到的消息为已读
     * 批量更新该用户在指定会话中所有未读消息的状态
     *
     * @param conversationId 会话id
     * @param userId         当前用户id
     */
    @Override
    public void markAsRead(Long conversationId, Long userId) {
        validateConversationAccess(conversationId, userId);
        UpdateChain.of(ChatMessage.class)
                .set("isRead", 1)
                .where("conversationId = ? AND receiverId = ? AND isRead = 0", conversationId, userId)
                .update();
    }

    /**
     * 获取用户总未读消息数
     *
     * @param userId 用户id
     * @return 未读消息总数
     */
    @Override
    public int getUnreadCount(Long userId) {
        QueryWrapper query = QueryWrapper.create()
                .from(ChatMessage.class)
                .where("receiverId = ? AND isRead = 0", userId);
        return (int) count(query);
    }

    private void validateConversationAccess(Long conversationId, Long currentUserId) {
        ChatConversation conversation = chatConversationService.getById(conversationId);
        if (conversation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "会话不存在");
        }
        boolean isParticipant = conversation.getUserIdSmall().equals(currentUserId)
                || conversation.getUserIdLarge().equals(currentUserId);
        if (!isParticipant) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该会话");
        }
    }

    private Map<Long, AppCollaborator> loadCollaboratorMap(List<ChatMessage> messages) {
        Set<Long> collaboratorIds = messages.stream()
                .filter(message -> "collab_invite".equals(message.getMessageType()))
                .map(this::extractCollabId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (collaboratorIds.isEmpty()) {
            return Map.of();
        }
        List<AppCollaborator> collaborators = appCollaboratorMapper.selectListByIds(new ArrayList<>(collaboratorIds));
        return collaborators.stream().collect(Collectors.toMap(AppCollaborator::getId, item -> item, (a, b) -> a));
    }

    private ChatMessageVO buildMessageVO(ChatMessage message, User sender, Map<Long, AppCollaborator> collaboratorMap) {
        ChatMessageVO vo = new ChatMessageVO();
        BeanUtil.copyProperties(message, vo);
        if (sender != null) {
            vo.setSenderName(sender.getUserName());
            vo.setSenderAvatar(sender.getUserAvatar());
        }
        if (!"collab_invite".equals(message.getMessageType())) {
            return vo;
        }
        JSONObject payload = parseCollabPayload(message.getContent());
        if (payload == null) {
            return vo;
        }
        Long collabId = payload.getLong("collabId");
        vo.setCollabId(collabId);
        vo.setAppId(payload.getLong("appId"));
        vo.setAppName(payload.getStr("appName"));
        vo.setAppCover(payload.getStr("appCover"));
        vo.setCollabRole(payload.getStr("role"));
        if (collabId != null) {
            AppCollaborator collaborator = collaboratorMap.get(collabId);
            if (collaborator != null) {
                vo.setCollabStatus(collaborator.getStatus());
            }
        }
        return vo;
    }

    private Long extractCollabId(ChatMessage message) {
        JSONObject payload = parseCollabPayload(message.getContent());
        return payload == null ? null : payload.getLong("collabId");
    }

    private JSONObject parseCollabPayload(String content) {
        if (content == null || content.isBlank() || !JSONUtil.isTypeJSON(content)) {
            return null;
        }
        try {
            return JSONUtil.parseObj(content);
        } catch (Exception e) {
            return null;
        }
    }
}
