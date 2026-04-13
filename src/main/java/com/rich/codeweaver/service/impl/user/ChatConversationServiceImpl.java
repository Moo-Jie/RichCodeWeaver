package com.rich.codeweaver.service.impl.user;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.codeweaver.common.constant.ChatConstant;
import com.rich.codeweaver.model.entity.ChatConversation;
import com.rich.codeweaver.model.entity.ChatMessage;
import com.rich.codeweaver.model.entity.User;
import com.rich.codeweaver.model.vo.ChatConversationVO;
import com.rich.codeweaver.mapper.ChatConversationMapper;
import com.rich.codeweaver.mapper.ChatMessageMapper;
import com.rich.codeweaver.mapper.UserMapper;
import com.rich.codeweaver.service.user.ChatConversationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 聊天会话 服务实现
 * 实现会话的创建、查询等核心业务逻辑
 * userIdSmall < userIdLarge 规则保证每对用户唯一一条会话记录
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@Service
public class ChatConversationServiceImpl extends ServiceImpl<ChatConversationMapper, ChatConversation>
        implements ChatConversationService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private ChatMessageMapper chatMessageMapper;

    /**
     * 获取或创建两个用户之间的会话
     * 使用 userIdSmall < userIdLarge 规则确保唯一性
     *
     * @param userId1 用户1的id
     * @param userId2 用户2的id
     * @return 会话实体
     */
    @Override
    public ChatConversation getOrCreateConversation(Long userId1, Long userId2) {
        Long small = Math.min(userId1, userId2);
        Long large = Math.max(userId1, userId2);

        // 查找已有会话
        QueryWrapper query = QueryWrapper.create()
                .from(ChatConversation.class)
                .where("userIdSmall = ? AND userIdLarge = ?", small, large);
        ChatConversation conversation = getOne(query);

        if (conversation == null) {
            // 创建新会话
            conversation = ChatConversation.builder()
                    .userIdSmall(small)
                    .userIdLarge(large)
                    .build();
            save(conversation);
        }
        return conversation;
    }

    /**
     * 获取用户的所有会话列表（按最后消息时间倒序）
     * 附带对方用户信息、最后消息内容预览、未读消息数
     *
     * @param userId 当前用户id
     * @return 会话列表VO
     */
    @Override
    public List<ChatConversationVO> listConversations(Long userId) {
        // 查询该用户参与的所有会话
        QueryWrapper query = QueryWrapper.create()
                .from(ChatConversation.class)
                .where("userIdSmall = ? OR userIdLarge = ?", userId, userId)
                .orderBy("lastMessageTime", false);
        List<ChatConversation> conversations = list(query);
        if (conversations.isEmpty()) {
            return new ArrayList<>();
        }

        // 收集对方用户id
        Set<Long> targetUserIds = conversations.stream()
                .map(c -> c.getUserIdSmall().equals(userId) ? c.getUserIdLarge() : c.getUserIdSmall())
                .collect(Collectors.toSet());
        // 批量查询用户信息
        List<User> users = userMapper.selectListByIds(new ArrayList<>(targetUserIds));
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 收集最后消息id
        Set<Long> messageIds = conversations.stream()
                .filter(c -> c.getLastMessageId() != null)
                .map(ChatConversation::getLastMessageId)
                .collect(Collectors.toSet());
        Map<Long, ChatMessage> messageMap = Map.of();
        if (!messageIds.isEmpty()) {
            List<ChatMessage> messages = chatMessageMapper.selectListByIds(new ArrayList<>(messageIds));
            messageMap = messages.stream()
                    .collect(Collectors.toMap(ChatMessage::getId, m -> m));
        }

        // 组装VO
        Map<Long, ChatMessage> finalMessageMap = messageMap;
        return conversations.stream().map(c -> {
            ChatConversationVO vo = new ChatConversationVO();
            vo.setId(c.getId());
            vo.setCreateTime(c.getCreateTime());
            vo.setLastMessageTime(c.getLastMessageTime());

            // 对方用户信息
            Long targetId = c.getUserIdSmall().equals(userId) ? c.getUserIdLarge() : c.getUserIdSmall();
            vo.setTargetUserId(targetId);
            User targetUser = userMap.get(targetId);
            if (targetUser != null) {
                vo.setTargetUserName(targetUser.getUserName());
                vo.setTargetUserAvatar(targetUser.getUserAvatar());
            }

            // 最后消息预览
            if (c.getLastMessageId() != null) {
                ChatMessage lastMsg = finalMessageMap.get(c.getLastMessageId());
                if (lastMsg != null) {
                    vo.setLastMessageType(lastMsg.getMessageType());
                    // 特殊消息类型使用友好的预览文本
                    if (ChatConstant.MESSAGE_TYPE_COLLAB_INVITE.equals(lastMsg.getMessageType())) {
                        vo.setLastMessageContent(ChatConstant.CONVERSATION_PREVIEW_COLLAB_INVITE);
                    } else if (ChatConstant.MESSAGE_TYPE_APP_FORWARD.equals(lastMsg.getMessageType())) {
                        vo.setLastMessageContent(ChatConstant.CONVERSATION_PREVIEW_APP_FORWARD);
                    } else {
                        String content = lastMsg.getContent();
                        vo.setLastMessageContent(content != null && content.length() > 50
                                ? content.substring(0, 50) + "..." : content);
                    }
                }
            }

            // 未读消息数
            QueryWrapper unreadQuery = QueryWrapper.create()
                    .from(ChatMessage.class)
                    .where("conversationId = ? AND receiverId = ? AND isRead = 0", c.getId(), userId);
            long unread = chatMessageMapper.selectCountByQuery(unreadQuery);
            vo.setUnreadCount((int) unread);

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 更新会话的最后消息信息
     *
     * @param conversationId 会话id
     * @param messageId      消息id
     */
    @Override
    public void updateLastMessage(Long conversationId, Long messageId) {
        ChatConversation conversation = getById(conversationId);
        if (conversation != null) {
            conversation.setLastMessageId(messageId);
            conversation.setLastMessageTime(LocalDateTime.now());
            updateById(conversation);
        }
    }
}
