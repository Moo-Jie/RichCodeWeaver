package com.rich.user.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.common.constant.ChatConstant;
import com.rich.model.entity.ChatConversation;
import com.rich.model.entity.ChatMessage;
import com.rich.model.entity.User;
import com.rich.model.vo.ChatConversationVO;
import com.rich.user.mapper.ChatConversationMapper;
import com.rich.user.mapper.ChatMessageMapper;
import com.rich.user.mapper.UserMapper;
import com.rich.user.service.ChatConversationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 聊天会话服务实现。
 * userIdSmall < userIdLarge 规则保证每对用户唯一一条会话记录。
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

    @Override
    public ChatConversation getOrCreateConversation(Long userId1, Long userId2) {
        Long small = Math.min(userId1, userId2);
        Long large = Math.max(userId1, userId2);
        QueryWrapper query = QueryWrapper.create()
                .from(ChatConversation.class)
                .where("userIdSmall = ? AND userIdLarge = ?", small, large);
        ChatConversation conversation = getOne(query);
        if (conversation == null) {
            conversation = ChatConversation.builder()
                    .userIdSmall(small)
                    .userIdLarge(large)
                    .build();
            try {
                save(conversation);
            } catch (RuntimeException e) {
                // 并发创建首条消息时，唯一键冲突方回查已创建会话。
                conversation = getOne(query);
                if (conversation == null) {
                    throw e;
                }
            }
        }
        return conversation;
    }

    @Override
    public List<ChatConversationVO> listConversations(Long userId) {
        QueryWrapper query = QueryWrapper.create()
                .from(ChatConversation.class)
                .where("userIdSmall = ? OR userIdLarge = ?", userId, userId)
                .orderBy("lastMessageTime", false)
                .orderBy("lastMessageId", false);
        List<ChatConversation> conversations = list(query);
        if (conversations.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> targetUserIds = conversations.stream()
                .map(c -> c.getUserIdSmall().equals(userId) ? c.getUserIdLarge() : c.getUserIdSmall())
                .collect(Collectors.toSet());
        List<User> users = userMapper.selectListByIds(new ArrayList<>(targetUserIds));
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

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

        // 一次查询并聚合所有会话未读数，避免按会话执行 COUNT 的 N+1。
        List<Long> conversationIds = conversations.stream()
                .map(ChatConversation::getId)
                .collect(Collectors.toList());
        List<ChatMessage> unreadMessages = chatMessageMapper.selectListByQuery(
                QueryWrapper.create()
                        .from(ChatMessage.class)
                        .where("receiverId = ? AND isRead = 0", userId)
                        .in("conversationId", conversationIds));
        Map<Long, Long> unreadMap = new HashMap<>();
        for (ChatMessage unreadMessage : unreadMessages) {
            unreadMap.merge(unreadMessage.getConversationId(), 1L, Long::sum);
        }

        Map<Long, ChatMessage> finalMessageMap = messageMap;
        return conversations.stream().map(c -> {
            ChatConversationVO vo = new ChatConversationVO();
            vo.setId(c.getId());
            vo.setCreateTime(c.getCreateTime());
            vo.setLastMessageTime(c.getLastMessageTime());

            Long targetId = c.getUserIdSmall().equals(userId) ? c.getUserIdLarge() : c.getUserIdSmall();
            vo.setTargetUserId(targetId);
            User targetUser = userMap.get(targetId);
            if (targetUser != null) {
                vo.setTargetUserName(targetUser.getUserName());
                vo.setTargetUserAvatar(targetUser.getUserAvatar());
            }

            if (c.getLastMessageId() != null) {
                ChatMessage lastMsg = finalMessageMap.get(c.getLastMessageId());
                if (lastMsg != null) {
                    vo.setLastMessageType(lastMsg.getMessageType());
                    if (ChatConstant.MESSAGE_TYPE_COLLAB_INVITE.equals(lastMsg.getMessageType())) {
                        vo.setLastMessageContent(ChatConstant.CONVERSATION_PREVIEW_COLLAB_INVITE);
                    } else if (ChatConstant.MESSAGE_TYPE_APP_FORWARD.equals(lastMsg.getMessageType())) {
                        vo.setLastMessageContent(ChatConstant.CONVERSATION_PREVIEW_APP_FORWARD);
                    } else {
                        String content = lastMsg.getContent();
                        vo.setLastMessageContent(content != null && content.length() > 50
                                ? content.substring(0, 50) + "..." : content);
                    }
                    if (lastMsg.getCreateTime() != null) {
                        vo.setLastMessageTime(lastMsg.getCreateTime());
                    }
                }
            }

            vo.setUnreadCount(unreadMap.getOrDefault(c.getId(), 0L).intValue());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void updateLastMessage(Long conversationId, Long messageId) {
        ChatConversation conversation = getById(conversationId);
        ChatMessage message = chatMessageMapper.selectOneById(messageId);
        if (conversation != null && message != null
                && (conversation.getLastMessageId() == null || messageId > conversation.getLastMessageId())) {
            // 使用消息实际创建时间，避免排序时间与最后消息内容错位。
            conversation.setLastMessageId(messageId);
            conversation.setLastMessageTime(message.getCreateTime());
            updateById(conversation);
        }
    }
}
