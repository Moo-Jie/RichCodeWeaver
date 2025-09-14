package com.rich.richcodeweaver.utils;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring 上下文工具类（用于在静态方法中获取Spring Bean）
 * 实现 ApplicationContextAware 接口获取 Spring 应用上下文
 * 通过静态方法提供 Bean 获取能力
 *
 * @author DuRuiChi
 * @create 2025/9/14
 **/
@Component
public class SpringContextUtil implements ApplicationContextAware {

    // 静态持有的 Spring 应用上下文（由 Spring 容器自动注入）
    private static ApplicationContext applicationContext;

    /**
     * 通过类型获取 Spring Bean
     *
     * @param clazz Bean 的类类型
     * @return 匹配类型的 Bean 实例
     */
    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }

    /**
     * 通过 Bean 名称获取 Spring Bean
     *
     * @param name Bean 的注册名称
     * @return 匹配名称的 Bean 实例
     */
    public static Object getBean(String name) {
        return applicationContext.getBean(name);
    }

    /**
     * 通过名称和类型双重匹配获取 Spring Bean
     *
     * @param name  Bean 的注册名称
     * @param clazz Bean 的类类型
     * @return 匹配名称和类型的 Bean 实例
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return applicationContext.getBean(name, clazz);
    }

    /**
     * Spring 容器初始化时自动调用此方法注入静态上下文
     *
     * @param applicationContext Spring 应用上下文对象
     */
    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        SpringContextUtil.applicationContext = applicationContext;
    }
}