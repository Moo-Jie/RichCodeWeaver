package com.rich.codeweaver.websocket;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置类
 * 注册聊天WebSocket处理器，配置允许的跨域来源
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private ChatWebSocketHandler chatWebSocketHandler;

    @Resource
    private ChatWebSocketInterceptor chatWebSocketInterceptor;

    /**
     * 注册WebSocket处理器
     * 端点: /user/ws/chat（最终对外路径会自动带上 context-path=/api）
     * 允许所有来源的跨域请求（生产环境应限制来源）
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册处理器，用于处理 /user/ws/chat 路径的连接
        registry.addHandler(chatWebSocketHandler, "/user/ws/chat")
                // 注册监听器
                .addInterceptors(chatWebSocketInterceptor)
                .setAllowedOrigins("*");
    }
}