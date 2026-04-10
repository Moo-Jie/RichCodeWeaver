package com.rich.user.websocket;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rich.common.constant.ChatConstant;
import com.rich.model.vo.ChatMessageVO;
import com.rich.user.service.ChatMessageService;
import com.rich.user.service.UserFriendshipService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天 WebSocket 处理器
 * 管理用户WebSocket连接，处理实时消息的发送与接收
 * 维护在线用户会话映射，支持消息的实时推送
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    /**
     * 在线用户会话映射: userId -> WebSocketSession
     */
    private static final ConcurrentHashMap<Long, WebSocketSession> ONLINE_USERS = new ConcurrentHashMap<>();

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private UserFriendshipService userFriendshipService;

    /**
     * WebSocket连接建立后，将用户加入在线用户的映射表中
     *
     * @param session WebSocket会话
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserId(session);
        if (userId != null) {
            ONLINE_USERS.put(userId, session);
            log.info("[WebSocket] 用户上线: userId={}", userId);
            // 发送未读消息数
            sendUnreadCount(userId, session);
        }
    }

    /**
     * 处理收到的文本消息
     * 消息格式（JSON）:
     * - type: "chat" 发送聊天消息
     *   payload: { receiverId, content, messageType }
     * - type: "read" 标记消息已读
     *   payload: { conversationId }
     *
     * @param session WebSocket会话
     * @param message 文本消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long senderId = getUserId(session);
        if (senderId == null) {
            return;
        }
        try {
            JSONObject json = JSONUtil.parseObj(message.getPayload());
            String type = json.getStr("type");
            JSONObject payload = json.getJSONObject("payload");

            switch (type) {
                case "chat" -> handleChatMessage(senderId, payload);
                case "read" -> handleReadMessage(senderId, payload);
                default -> log.warn("[WebSocket] 未知消息类型: type={}", type);
            }
        } catch (Exception e) {
            log.error("[WebSocket] 消息处理异常: userId={}", senderId, e);
            sendError(session, "消息处理失败: " + e.getMessage());
        }
    }

    /**
     * WebSocket连接关闭后，将用户从在线映射中移除
     *
     * @param session WebSocket会话
     * @param status  关闭状态
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserId(session);
        if (userId != null) {
            ONLINE_USERS.remove(userId);
            log.info("[WebSocket] 用户下线: userId={}, status={}", userId, status);
        }
    }

    /**
     * WebSocket传输出错时的处理
     *
     * @param session   WebSocket会话
     * @param exception 异常信息
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long userId = getUserId(session);
        log.error("[WebSocket] 传输错误: userId={}", userId, exception);
        ONLINE_USERS.remove(userId);
    }

    // ===== 消息处理方法 =====

    /**
     * 处理聊天消息：校验好友关系、保存消息、推送给接收方
     *
     * @param senderId 发送方用户id
     * @param payload  消息载荷 { receiverId, content, messageType }
     */
    private void handleChatMessage(Long senderId, JSONObject payload) {
        Long receiverId = payload.getLong("receiverId");
        String content = payload.getStr("content");
        String messageType = payload.getStr("messageType", ChatConstant.MESSAGE_TYPE_TEXT);

        // 校验好友关系
        if (!userFriendshipService.isFriend(senderId, receiverId)) {
            WebSocketSession senderSession = ONLINE_USERS.get(senderId);
            if (senderSession != null) {
                sendError(senderSession, "对方不是您的好友，无法发送消息");
            }
            return;
        }

        // 保存消息到数据库
        ChatMessageVO messageVO = chatMessageService.sendMessage(senderId, receiverId, content, messageType);

        // 推送给接收方（如果在线）
        WebSocketSession receiverSession = ONLINE_USERS.get(receiverId);
        if (receiverSession != null && receiverSession.isOpen()) {
            JSONObject pushData = new JSONObject();
            pushData.set("type", "newMessage");
            pushData.set("data", messageVO);
            sendMessage(receiverSession, pushData.toString());
        }

        // 回复发送方确认
        WebSocketSession senderSession = ONLINE_USERS.get(senderId);
        if (senderSession != null && senderSession.isOpen()) {
            JSONObject confirmData = new JSONObject();
            confirmData.set("type", "messageSent");
            confirmData.set("data", messageVO);
            sendMessage(senderSession, confirmData.toString());
        }
    }

    /**
     * 处理已读标记：标记会话中的消息为已读
     *
     * @param userId  当前用户id
     * @param payload 载荷 { conversationId }
     */
    private void handleReadMessage(Long userId, JSONObject payload) {
        Long conversationId = payload.getLong("conversationId");
        if (conversationId != null) {
            chatMessageService.markAsRead(conversationId, userId);
        }
    }

    /**
     * 向接收方推送聊天消息（newMessage），同时向发送方推送确认（messageSent）
     * 用于非WebSocket发起的消息（如协作邀请等系统消息），走HTTP保存后通过此方法实时推送
     *
     * @param senderId  发送方用户id
     * @param receiverId 接收方用户id
     * @param messageVO 消息视图对象
     */
    public void pushChatMessage(Long senderId, Long receiverId, ChatMessageVO messageVO) {
        // 推送给接收方
        WebSocketSession receiverSession = ONLINE_USERS.get(receiverId);
        if (receiverSession != null && receiverSession.isOpen()) {
            JSONObject pushData = new JSONObject();
            pushData.set("type", "newMessage");
            pushData.set("data", messageVO);
            sendMessage(receiverSession, pushData.toString());
        }
        // 推送给发送方
        WebSocketSession senderSession = ONLINE_USERS.get(senderId);
        if (senderSession != null && senderSession.isOpen()) {
            JSONObject confirmData = new JSONObject();
            confirmData.set("type", "messageSent");
            confirmData.set("data", messageVO);
            sendMessage(senderSession, confirmData.toString());
        }
    }

    /**
     * 向指定用户推送好友申请通知
     *
     * @param userId 目标用户id
     * @param data   通知数据
     */
    public void pushFriendNotification(Long userId, Object data) {
        WebSocketSession session = ONLINE_USERS.get(userId);
        if (session != null && session.isOpen()) {
            JSONObject pushData = new JSONObject();
            pushData.set("type", "friendNotification");
            pushData.set("data", data);
            sendMessage(session, pushData.toString());
        }
    }

    /**
     * 向指定用户推送协作邀请通知
     * 通知类型: collabNotification
     * data.action: invite(收到邀请) / accepted(对方接受) / rejected(对方拒绝)
     *
     * @param userId 目标用户id
     * @param data   通知数据
     */
    public void pushCollabNotification(Long userId, Object data) {
        WebSocketSession session = ONLINE_USERS.get(userId);
        if (session != null && session.isOpen()) {
            JSONObject pushData = new JSONObject();
            pushData.set("type", "collabNotification");
            pushData.set("data", data);
            sendMessage(session, pushData.toString());
        }
    }

    /**
     * 发送未读消息数给用户
     *
     * @param userId  用户id
     * @param session WebSocket会话
     */
    private void sendUnreadCount(Long userId, WebSocketSession session) {
        int unread = chatMessageService.getUnreadCount(userId);
        JSONObject data = new JSONObject();
        data.set("type", "unreadCount");
        data.set("data", unread);
        sendMessage(session, data.toString());
    }

    /**
     * 发送错误消息给客户端
     *
     * @param session WebSocket会话
     * @param error   错误信息
     */
    private void sendError(WebSocketSession session, String error) {
        JSONObject data = new JSONObject();
        data.set("type", "error");
        data.set("message", error);
        sendMessage(session, data.toString());
    }

    /**
     * 安全发送WebSocket消息，捕获IO异常
     *
     * @param session WebSocket会话
     * @param text    消息文本
     */
    private void sendMessage(WebSocketSession session, String text) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(text));
            }
        } catch (IOException e) {
            log.error("[WebSocket] 消息发送失败", e);
        }
    }

    /**
     * 从WebSocket会话属性中获取用户id
     *
     * @param session WebSocket会话
     * @return 用户id，未找到返回null
     */
    private Long getUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get(ChatConstant.WS_SESSION_USER_ID);
        return userId instanceof Long ? (Long) userId : null;
    }
}
