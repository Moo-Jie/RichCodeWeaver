package com.rich.richcodeweaver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局跨域配置
 *
 * @author DuRuiChi
 * @create 2025/8/4
 **/
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * 跨域配置
     *
     * @param registry 跨域注册对象
     * @author DuRuiChi
     * @create 2025/8/4
     **/
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowCredentials(true)
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*");
    }
}