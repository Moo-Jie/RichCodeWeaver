package com.rich.richcodeweaver.config.websocket;

import com.rich.richcodeweaver.websocket.CodeGenWebSocketHandler;
import com.rich.richcodeweaver.websocket.LoginUserHandshakeInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private CodeGenWebSocketHandler codeGenWebSocketHandler;

    @Resource
    private LoginUserHandshakeInterceptor loginUserHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 实际访问路径：/api/ws/codegen（因为 server.servlet.context-path=/api）
        registry.addHandler(codeGenWebSocketHandler, "/ws/codegen")
                .addInterceptors(new HttpSessionHandshakeInterceptor(), loginUserHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}


