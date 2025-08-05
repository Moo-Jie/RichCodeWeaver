package com.rich.richcodeweaver.config;

import com.rich.richcodeweaver.service.AiCodeGeneratorService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 代码生成器服务工厂，用于自动创建 AI 服务 Bean，使用 AI 服务直接注入即可
 *
 * @author DuRuiChi
 * @create 2025/8/5
 **/
@Configuration
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;

    /**
     * 创建 AI 代码生成器服务
     *
     * @return com.rich.richcodeweaver.service.AiCodeGeneratorService
     * @author DuRuiChi
     * @create 2025/8/5
     **/
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        /*
          create 主要作用：
          1.动态代理创建：为 AiCodeGeneratorService 接口生成实现类
          2.AI能力注入：将 chatModel 配置的大语言模型与接口方法绑定
          3.注解解析：自动解析接口方法上的SystemMessage等 LangChain4J 注解
         */
        return AiServices.create(AiCodeGeneratorService.class, chatModel);
    }
}
