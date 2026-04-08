package com.rich.user.websocket;

import com.rich.model.entity.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

import static com.rich.common.constant.UserConstant.USER_LOGIN_STATE;

/**
 * WebSocket 握手拦截器
 * 在WebSocket连接建立前从HTTP Session中提取登录用户信息
 * 将用户id存入WebSocket会话属性，供后续消息处理使用
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@Component
public class ChatWebSocketInterceptor implements HandshakeInterceptor {

    /**
     * WebSocket会话属性中存储用户id的key
     */
    public static final String WS_USER_ID = "wsUserId";

    /**
     * 握手前拦截：从HTTP Session中获取登录用户id
     * 未登录用户将被拒绝连接
     *
     * @param request    HTTP请求
     * @param response   HTTP响应
     * @param wsHandler  WebSocket处理器
     * @param attributes WebSocket会话属性（将传递给WebSocketSession）
     * @return true=允许握手, false=拒绝连接
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpSession session = servletRequest.getServletRequest().getSession(false);
            if (session != null) {
                Object userObj = session.getAttribute(USER_LOGIN_STATE);
                if (userObj instanceof User user && user.getId() != null) {
                    attributes.put(WS_USER_ID, user.getId());
                    return true;
                }
            }
        }
        // 未登录用户拒绝WebSocket连接
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手完成后无需额外处理
    }
}
