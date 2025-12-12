package com.rich.richcodeweaverserveradmin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 配置跨域访问等
 * 
 * @author Rich
 * @date 2025-12-12
 */
@Configuration
public class SecurityConfig implements WebMvcConfigurer {

    /**
     * 配置跨域访问
     * 允许前端页面访问Admin Server
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

