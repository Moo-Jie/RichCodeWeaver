package com.rich.app.mq.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket配置类
 * 用于任务进度实时推送
 *
 * @author DuRuiChi
 * @create 2026-05-06
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 配置消息代理
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用简单消息代理，用于向客户端推送消息
        registry.enableSimpleBroker("/topic", "/queue");
        
        // 设置应用目的地前缀，客户端发送消息时需要加此前缀
        registry.setApplicationDestinationPrefixes("/app");
        
        // 设置用户目的地前缀(点对点消息)
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * 注册STOMP端点
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册STOMP端点，客户端通过此端点连接WebSocket
        registry.addEndpoint("/generator/ws/task")
                .setAllowedOriginPatterns("*") // 允许跨域
                .withSockJS(); // 启用SockJS降级支持
    }
}
