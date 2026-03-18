package com.rich.app.aiTools;

import com.rich.ai.aiTools.webOperate.AiWebSearchTool;
import com.rich.app.AppApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 百度千帆搜索工具测试类
 * 用于手动测试百度千帆平台搜索API的调用
 * 
 * 配置要求：
 * 需要在 application-local.yml 中配置：
 * baidu.qianfan.appbuilder-api-key
 * 
 * 获取API Key：https://console.bce.baidu.com/qianfan/ais/console/applicationConsole/application
 *
 * @author DuRuiChi
 * @create 2026/3/18
 */
@Slf4j
@SpringBootTest(classes = AppApplication.class)
public class AiWebSearchToolTest {

    @Autowired
    private AiWebSearchTool aiWebSearchTool;

    /**
     * 测试基本搜索功能
     */
    @Test
    public void testBasicSearch() {
        log.info("========================================");
        log.info("开始测试：基本搜索功能");
        log.info("========================================");
        
        String query = "Java Spring Boot 最佳实践";
        log.info("搜索关键词: {}", query);
        
        String result = aiWebSearchTool.searchWeb(query);
        
        log.info("========================================");
        log.info("搜索结果:");
        log.info("{}", result);
        log.info("========================================");
    }

    /**
     * 测试英文搜索
     */
    @Test
    public void testEnglishSearch() {
        log.info("========================================");
        log.info("开始测试：英文搜索");
        log.info("========================================");
        
        String query = "React best practices 2024";
        log.info("搜索关键词: {}", query);
        
        String result = aiWebSearchTool.searchWeb(query);
        
        log.info("========================================");
        log.info("搜索结果:");
        log.info("{}", result);
        log.info("========================================");
    }

    /**
     * 测试技术相关搜索
     */
    @Test
    public void testTechSearch() {
        log.info("========================================");
        log.info("开始测试：技术相关搜索");
        log.info("========================================");
        
        String query = "Vue3 Composition API 教程";
        log.info("搜索关键词: {}", query);
        
        String result = aiWebSearchTool.searchWeb(query);
        
        log.info("========================================");
        log.info("搜索结果:");
        log.info("{}", result);
        log.info("========================================");
    }

    /**
     * 测试前端相关搜索（模拟工作流场景）
     */
    @Test
    public void testFrontendSearch() {
        log.info("========================================");
        log.info("开始测试：前端知识学习应用搜索");
        log.info("========================================");
        
        String query = "frontend educational SPA HTML CSS JS cards code highlight live preview";
        log.info("搜索关键词: {}", query);
        
        String result = aiWebSearchTool.searchWeb(query);
        
        log.info("========================================");
        log.info("搜索结果:");
        log.info("{}", result);
        log.info("========================================");
    }

    /**
     * 测试空查询（应该抛出异常）
     */
    @Test
    public void testEmptyQuery() {
        log.info("========================================");
        log.info("开始测试：空查询异常处理");
        log.info("========================================");
        
        try {
            String result = aiWebSearchTool.searchWeb("");
            log.error("预期应该抛出异常，但返回了结果: {}", result);
        } catch (IllegalArgumentException e) {
            log.info("正确捕获异常: {}", e.getMessage());
        }
        
        log.info("========================================");
    }

    /**
     * 测试工具元信息
     */
    @Test
    public void testToolMetadata() {
        log.info("========================================");
        log.info("开始测试：工具元信息");
        log.info("========================================");
        
        log.info("工具名称: {}", aiWebSearchTool.getToolName());
        log.info("工具显示名称: {}", aiWebSearchTool.getToolDisplayName());
        
        log.info("========================================");
    }
}
