package com.rich.richcodeweaver.websocket;

import com.rich.richcodeweaver.model.entity.User;
import com.rich.richcodeweaver.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手阶段鉴权：复用现有登录态（cookie/session）
 */
@Slf4j
@Component
public class LoginUserHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_USER_ID = "RCW_USER_ID";

    @Resource
    private UserService userService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletServerHttpRequest)) {
            return false;
        }
        HttpServletRequest httpServletRequest = servletServerHttpRequest.getServletRequest();
        try {
            User loginUser = userService.getLoginUser(httpServletRequest);
            attributes.put(ATTR_USER_ID, loginUser.getId());
            return true;
        } catch (Exception e) {
            log.warn("WebSocket handshake auth failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}


