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
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天 WebSocket 处理器。
 * 同一用户支持多个在线标签页，并通过客户端消息 ID 关联发送确认与错误。
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final int SEND_TIME_LIMIT_MS = 10_000;
    private static final int SEND_BUFFER_LIMIT_BYTES = 512 * 1024;
    private static final int CLIENT_MESSAGE_CACHE_LIMIT = 4096;

    /**
     * 在线用户会话映射: userId -> sessions。
     */
    private static final ConcurrentHashMap<Long, Set<WebSocketSession>> ONLINE_USERS =
            new ConcurrentHashMap<>();

    /**
     * 原始 sessionId -> 串行发送装饰器，关闭连接时据此精确移除。
     */
    private static final ConcurrentHashMap<String, WebSocketSession> SAFE_SESSIONS =
            new ConcurrentHashMap<>();

    /**
     * 进程内短期幂等缓存。数据库尚未增加 clientMessageId 唯一键时，
     * 可避免同一在线进程内的超时重试产生重复消息。
     */
    private static final ConcurrentHashMap<String, ChatMessageVO> CLIENT_MESSAGE_CACHE =
            new ConcurrentHashMap<>();

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private UserFriendshipService userFriendshipService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            return;
        }
        WebSocketSession safeSession = new ConcurrentWebSocketSessionDecorator(
                session, SEND_TIME_LIMIT_MS, SEND_BUFFER_LIMIT_BYTES);
        SAFE_SESSIONS.put(session.getId(), safeSession);
        ONLINE_USERS.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(safeSession);
        log.info("[WebSocket] 用户上线: userId={}, sessionId={}", userId, session.getId());
        sendUnreadCount(userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long senderId = getUserId(session);
        if (senderId == null) {
            return;
        }
        String clientMessageId = null;
        try {
            JSONObject json = JSONUtil.parseObj(message.getPayload());
            String type = json.getStr("type");
            JSONObject payload = json.getJSONObject("payload");
            if (payload != null) {
                clientMessageId = payload.getStr("clientMessageId");
            }
            switch (type) {
                case "chat" -> handleChatMessage(session, senderId, payload, clientMessageId);
                case "read" -> handleReadMessage(session, senderId, payload, clientMessageId);
                default -> sendError(session, "未知消息类型", clientMessageId);
            }
        } catch (Exception e) {
            log.error("[WebSocket] 消息处理异常: userId={}, clientMessageId={}",
                    senderId, clientMessageId, e);
            sendError(session, "消息处理失败: " + e.getMessage(), clientMessageId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        removeSession(session);
        log.info("[WebSocket] 连接关闭: sessionId={}, status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("[WebSocket] 传输错误: sessionId={}", session.getId(), exception);
        removeSession(session);
    }

    private void removeSession(WebSocketSession rawSession) {
        WebSocketSession safeSession = SAFE_SESSIONS.remove(rawSession.getId());
        if (safeSession == null) {
            return;
        }
        Long userId = getUserId(rawSession);
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> sessions = ONLINE_USERS.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.remove(safeSession);
        if (sessions.isEmpty()) {
            ONLINE_USERS.remove(userId, sessions);
            log.info("[WebSocket] 用户离线: userId={}", userId);
        }
    }

    private void handleChatMessage(WebSocketSession session, Long senderId, JSONObject payload,
                                   String clientMessageId) {
        if (payload == null) {
            sendError(session, "消息载荷不能为空", clientMessageId);
            return;
        }
        Long receiverId = payload.getLong("receiverId");
        String content = payload.getStr("content");
        String messageType = payload.getStr("messageType", ChatConstant.MESSAGE_TYPE_TEXT);

        if (receiverId == null || receiverId <= 0) {
            sendError(session, "接收方用户id无效", clientMessageId);
            return;
        }
        if (!ChatConstant.MESSAGE_TYPE_TEXT.equals(messageType)) {
            sendError(session, "普通聊天仅支持文本消息", clientMessageId);
            return;
        }
        if (content == null || content.trim().isEmpty() || content.trim().length() > 5000) {
            sendError(session, "消息内容长度必须在 1-5000 个字符之间", clientMessageId);
            return;
        }
        if (!userFriendshipService.isFriend(senderId, receiverId)) {
            sendError(session, "对方不是您的好友，无法发送消息", clientMessageId);
            return;
        }

        String dedupKey = clientMessageId == null ? null : senderId + ":" + clientMessageId;
        ChatMessageVO cached = dedupKey == null ? null : CLIENT_MESSAGE_CACHE.get(dedupKey);
        if (cached != null) {
            sendConfirmation(session, cached, clientMessageId);
            return;
        }

        ChatMessageVO messageVO = chatMessageService.sendMessage(
                senderId, receiverId, content, ChatConstant.MESSAGE_TYPE_TEXT);
        if (dedupKey != null) {
            if (CLIENT_MESSAGE_CACHE.size() >= CLIENT_MESSAGE_CACHE_LIMIT) {
                CLIENT_MESSAGE_CACHE.clear();
            }
            CLIENT_MESSAGE_CACHE.put(dedupKey, messageVO);
        }

        broadcastToUser(receiverId, "newMessage", messageVO);
        sendConfirmation(session, messageVO, clientMessageId);
        sendUnreadCount(receiverId);
    }

    private void handleReadMessage(WebSocketSession session, Long userId, JSONObject payload, String clientMessageId) {
        if (payload == null) {
            sendError(session, "已读载荷不能为空", clientMessageId);
            return;
        }
        Long conversationId = payload.getLong("conversationId");
        Long lastReadMessageId = payload.getLong("lastReadMessageId");
        if (conversationId == null || conversationId <= 0) {
            return;
        }
        chatMessageService.markAsRead(conversationId, userId, lastReadMessageId);
        sendUnreadCount(userId);
    }

    /**
     * 供 HTTP 降级发送或内部系统消息复用：落库后补齐实时推送。
     */
    public void pushChatMessage(Long senderId, Long receiverId, ChatMessageVO messageVO) {
        broadcastToUser(receiverId, "newMessage", messageVO);
        broadcastToUser(senderId, "messageSent", messageVO);
        sendUnreadCount(receiverId);
    }

    public void pushFriendNotification(Long userId, Object data) {
        broadcastToUser(userId, "friendNotification", data);
    }

    public void pushCollabNotification(Long userId, Object data) {
        broadcastToUser(userId, "collabNotification", data);
    }

    public void pushUnreadCount(Long userId) {
        sendUnreadCount(userId);
    }

    private void sendUnreadCount(Long userId) {
        Set<WebSocketSession> sessions = ONLINE_USERS.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        int unread = chatMessageService.getUnreadCount(userId);
        JSONObject data = new JSONObject();
        data.set("type", "unreadCount");
        data.set("data", unread);
        broadcast(userId, data.toString());
    }

    private void sendConfirmation(WebSocketSession session, ChatMessageVO messageVO, String clientMessageId) {
        JSONObject confirmData = new JSONObject();
        confirmData.set("type", "messageSent");
        confirmData.set("data", messageVO);
        if (clientMessageId != null && !clientMessageId.isBlank()) {
            confirmData.set("clientMessageId", clientMessageId);
        }
        sendMessage(session, confirmData.toString());
    }

    private void broadcastToUser(Long userId, String type, Object data) {
        JSONObject envelope = new JSONObject();
        envelope.set("type", type);
        envelope.set("data", data);
        broadcast(userId, envelope.toString());
    }

    private void broadcast(Long userId, String text) {
        Set<WebSocketSession> sessions = ONLINE_USERS.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        for (WebSocketSession session : sessions) {
            sendMessage(session, text);
        }
    }

    private void sendError(WebSocketSession session, String error, String clientMessageId) {
        if (session == null) {
            log.warn("[WebSocket] 无法发送错误消息，session 为空: {}", error);
            return;
        }
        JSONObject data = new JSONObject();
        data.set("type", "error");
        data.set("message", error);
        if (clientMessageId != null && !clientMessageId.isBlank()) {
            data.set("clientMessageId", clientMessageId);
        }
        sendMessage(session, data.toString());
    }

    private WebSocketSession safeSession(WebSocketSession rawSession) {
        if (rawSession == null) {
            return null;
        }
        return SAFE_SESSIONS.getOrDefault(rawSession.getId(), rawSession);
    }

    private void sendMessage(WebSocketSession session, String text) {
        WebSocketSession safeSession = safeSession(session);
        if (safeSession == null) {
            return;
        }
        try {
            if (safeSession.isOpen()) {
                safeSession.sendMessage(new TextMessage(text));
            }
        } catch (IOException | IllegalStateException e) {
            log.error("[WebSocket] 消息发送失败", e);
        }
    }

    private Long getUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get(ChatConstant.WS_SESSION_USER_ID);
        return userId instanceof Long ? (Long) userId : null;
    }
}
