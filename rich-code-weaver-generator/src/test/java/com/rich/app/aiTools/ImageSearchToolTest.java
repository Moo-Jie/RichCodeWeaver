package com.rich.app.aiTools;

import com.rich.ai.aiTools.ImageResource.ImageSearchTool;
import com.rich.app.GeneratorApplication;
import com.rich.model.entity.ImageResource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 接口盒子图片搜索工具测试类
 * 用于手动测试接口盒子（apihz.cn）图片搜索API的调用
 * 
 * 配置说明：
 * 默认使用公共测试ID和KEY（已在代码中配置）
 * 如需使用自己的账号，请在 application-local.yml 中配置：
 * apihz.image-search.id: 你的接口盒子ID
 * apihz.image-search.key: 你的开发者KEY
 * 
 * 注册地址：https://www.apihz.cn/user/
 *
 * @author DuRuiChi
 * @create 2026/3/18
 */
@Slf4j
@SpringBootTest(classes = GeneratorApplication.class)
public class ImageSearchToolTest {

    @Autowired
    private ImageSearchTool imageSearchTool;

    /**
     * 测试基本图片搜索功能
     */
    @Test
    public void testBasicImageSearch() {
        log.info("========================================");
        log.info("开始测试：基本图片搜索功能");
        log.info("========================================");
        
        String query = "风景";
        log.info("搜索关键词: {}", query);
        
        List<ImageResource> images = imageSearchTool.searchImages(query);
        
        log.info("========================================");
        log.info("搜索到 {} 张图片", images.size());
        for (int i = 0; i < images.size(); i++) {
            ImageResource img = images.get(i);
            log.info("图片 {}: ", i + 1);
            log.info("  - 分类: {}", img.getCategory());
            log.info("  - 描述: {}", img.getDescription());
            log.info("  - URL: {}", img.getUrl());
        }
        log.info("========================================");
    }

    /**
     * 测试技术类图片搜索
     */
    @Test
    public void testTechImageSearch() {
        log.info("========================================");
        log.info("开始测试：技术类图片搜索");
        log.info("========================================");
        
        String query = "编程代码";
        log.info("搜索关键词: {}", query);
        
        List<ImageResource> images = imageSearchTool.searchImages(query);
        
        log.info("========================================");
        log.info("搜索到 {} 张图片", images.size());
        for (int i = 0; i < images.size(); i++) {
            ImageResource img = images.get(i);
            log.info("图片 {}: ", i + 1);
            log.info("  - 分类: {}", img.getCategory());
            log.info("  - 描述: {}", img.getDescription());
            log.info("  - URL: {}", img.getUrl());
        }
        log.info("========================================");
    }

    /**
     * 测试UI设计类图片搜索
     */
    @Test
    public void testUIDesignImageSearch() {
        log.info("========================================");
        log.info("开始测试：UI设计类图片搜索");
        log.info("========================================");
        
        String query = "网页设计界面";
        log.info("搜索关键词: {}", query);
        
        List<ImageResource> images = imageSearchTool.searchImages(query);
        
        log.info("========================================");
        log.info("搜索到 {} 张图片", images.size());
        for (int i = 0; i < images.size(); i++) {
            ImageResource img = images.get(i);
            log.info("图片 {}: ", i + 1);
            log.info("  - 分类: {}", img.getCategory());
            log.info("  - 描述: {}", img.getDescription());
            log.info("  - URL: {}", img.getUrl());
        }
        log.info("========================================");
    }

    /**
     * 测试前端学习产物相关图片搜索（模拟工作流场景）
     */
    @Test
    public void testFrontendLearningImageSearch() {
        log.info("========================================");
        log.info("开始测试：前端学习产物图片搜索");
        log.info("========================================");
        
        String query = "HTML CSS JavaScript 教程";
        log.info("搜索关键词: {}", query);
        
        List<ImageResource> images = imageSearchTool.searchImages(query);
        
        log.info("========================================");
        log.info("搜索到 {} 张图片", images.size());
        for (int i = 0; i < images.size(); i++) {
            ImageResource img = images.get(i);
            log.info("图片 {}: ", i + 1);
            log.info("  - 分类: {}", img.getCategory());
            log.info("  - 描述: {}", img.getDescription());
            log.info("  - URL: {}", img.getUrl());
        }
        log.info("========================================");
    }

    /**
     * 测试英文关键词搜索
     */
    @Test
    public void testEnglishKeywordSearch() {
        log.info("========================================");
        log.info("开始测试：英文关键词搜索");
        log.info("========================================");
        
        String query = "web development";
        log.info("搜索关键词: {}", query);
        
        List<ImageResource> images = imageSearchTool.searchImages(query);
        
        log.info("========================================");
        log.info("搜索到 {} 张图片", images.size());
        for (int i = 0; i < images.size(); i++) {
            ImageResource img = images.get(i);
            log.info("图片 {}: ", i + 1);
            log.info("  - 分类: {}", img.getCategory());
            log.info("  - 描述: {}", img.getDescription());
            log.info("  - URL: {}", img.getUrl());
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
        
        log.info("工具名称: {}", imageSearchTool.getToolName());
        log.info("工具显示名称: {}", imageSearchTool.getToolDisplayName());
        
        log.info("========================================");
    }

    /**
     * 测试空结果处理
     */
    @Test
    public void testNoResultsHandling() {
        log.info("========================================");
        log.info("开始测试：空结果处理");
        log.info("========================================");
        
        String query = "xyzabc123nonexistent";
        log.info("搜索关键词（预期无结果）: {}", query);
        
        List<ImageResource> images = imageSearchTool.searchImages(query);
        
        log.info("========================================");
        log.info("搜索到 {} 张图片", images.size());
        log.info("空结果处理正常");
        log.info("========================================");
    }
}
