package com.rich.richcodeweaver.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rich.richcodeweaver.service.codegen.CodeGenTaskManager;
import com.rich.richcodeweaver.websocket.dto.CodeGenWsClientMessage;
import com.rich.richcodeweaver.websocket.dto.CodeGenWsServerMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

@Slf4j
@Component
public class CodeGenWebSocketHandler extends TextWebSocketHandler {

    @Resource
    private CodeGenTaskManager taskManager;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WS connected: id={}, userId={}", session.getId(), getUserId(session));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            log.info("WS recv: id={}, payload={}", session.getId(), message.getPayload());
            CodeGenWsClientMessage clientMsg = objectMapper.readValue(message.getPayload(), CodeGenWsClientMessage.class);
            String type = clientMsg.getType();

            Long userId = getUserId(session);
            if (userId == null) {
                taskManager.safeSend(session, CodeGenWsServerMessage.builder()
                        .type("error")
                        .message("未登录")
                        .build());
                session.close();
                return;
            }

            switch (type == null ? "" : type) {
                case "ping" -> taskManager.safeSend(session, CodeGenWsServerMessage.builder().type("pong").build());
                case "start" -> {
                    Long appId = parseAppId(clientMsg.getAppId());
                    if (appId == null) {
                        taskManager.safeSend(session, CodeGenWsServerMessage.builder()
                                .type("error")
                                .message("appId 非法")
                                .build());
                        return;
                    }
                    String prompt = clientMsg.getMessage();
                    boolean isAgent = Boolean.TRUE.equals(clientMsg.getIsAgent());
                    String taskId = taskManager.startOrGetActive(userId, appId, prompt, isAgent, session);
                    taskManager.safeSend(session, CodeGenWsServerMessage.builder()
                            .type("started")
                            .taskId(taskId)
                            .appId(String.valueOf(appId))
                            .build());
                }
                case "resume" -> {
                    String taskId = clientMsg.getTaskId();
                    Long appId = parseAppId(clientMsg.getAppId());
                    long fromSeq = clientMsg.getFromSeq() == null ? 1L : clientMsg.getFromSeq();
                    if (taskId == null || appId == null) {
                        taskManager.safeSend(session, CodeGenWsServerMessage.builder()
                                .type("error")
                                .message("resume 参数缺失")
                                .build());
                        return;
                    }
                    taskManager.resume(taskId, fromSeq, session, appId);
                }
                case "cancel" -> {
                    String taskId = clientMsg.getTaskId();
                    if (taskId != null) {
                        taskManager.cancel(taskId);
                    }
                }
                default -> taskManager.safeSend(session, CodeGenWsServerMessage.builder()
                        .type("error")
                        .message("不支持的消息类型: " + type)
                        .build());
            }
        } catch (Exception e) {
            log.error("WS handleTextMessage error: {}", e.getMessage(), e);
            taskManager.safeSend(session, CodeGenWsServerMessage.builder()
                    .type("error")
                    .message("服务端处理异常：" + e.getMessage())
                    .build());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WS closed: id={}, status={}", session.getId(), status);
        taskManager.unsubscribeAll(session);
    }

    private Long getUserId(WebSocketSession session) {
        Map<String, Object> attrs = session.getAttributes();
        Object val = attrs.get(LoginUserHandshakeInterceptor.ATTR_USER_ID);
        if (val == null) return null;
        try {
            return Long.parseLong(String.valueOf(val));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseAppId(String appIdStr) {
        if (appIdStr == null || appIdStr.isBlank()) return null;
        try {
            return Long.parseLong(appIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}


