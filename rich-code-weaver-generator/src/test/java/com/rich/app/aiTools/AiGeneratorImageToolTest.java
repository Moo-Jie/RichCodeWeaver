package com.rich.app.aiTools;

import com.rich.ai.aiTools.ImageResource.AiGeneratorImageTool;
import com.rich.app.GeneratorApplication;
import com.rich.model.entity.ImageResource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * AI图片生成工具测试类
 * 用于手动测试阿里云DashScope图片生成API的调用
 *
 * @author DuRuiChi
 * @create 2026/3/18
 */
@Slf4j
@SpringBootTest(classes = GeneratorApplication.class)
public class AiGeneratorImageToolTest {

    @Autowired
    private AiGeneratorImageTool aiGeneratorImageTool;

    /**
     * 测试Logo生成
     */
    @Test
    public void testLogoGeneration() {
        log.info("========================================");
        log.info("开始测试：Logo生成");
        log.info("========================================");

        String type = "Logo";
        String description = "科技公司Logo，蓝色调，简约现代风格，包含字母R和C";

        log.info("生成类型: {}", type);
        log.info("描述信息: {}", description);

        List<ImageResource> images = aiGeneratorImageTool.aiGeneratorImage(type, description);

        log.info("========================================");
        log.info("生成了 {} 张图片", images.size());
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
     * 测试图标生成
     */
    @Test
    public void testIconGeneration() {
        log.info("========================================");
        log.info("开始测试：图标生成");
        log.info("========================================");

        String type = "图标";
        String description = "代码编辑器图标，扁平化设计，深色背景，包含代码符号";

        log.info("生成类型: {}", type);
        log.info("描述信息: {}", description);

        List<ImageResource> images = aiGeneratorImageTool.aiGeneratorImage(type, description);

        log.info("========================================");
        log.info("生成了 {} 张图片", images.size());
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
     * 测试背景图生成
     */
    @Test
    public void testBackgroundGeneration() {
        log.info("========================================");
        log.info("开始测试：背景图生成");
        log.info("========================================");

        String type = "背景图";
        String description = "网站首页背景，渐变色，科技感，蓝紫色调，抽象几何图形";

        log.info("生成类型: {}", type);
        log.info("描述信息: {}", description);

        List<ImageResource> images = aiGeneratorImageTool.aiGeneratorImage(type, description);

        log.info("========================================");
        log.info("生成了 {} 张图片", images.size());
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
     * 测试概念视觉图生成
     */
    @Test
    public void testConceptVisualGeneration() {
        log.info("========================================");
        log.info("开始测试：概念视觉图生成");
        log.info("========================================");

        String type = "概念视觉图";
        String description = "前端开发学习平台，卡片式布局，代码编辑器界面，现代UI设计";

        log.info("生成类型: {}", type);
        log.info("描述信息: {}", description);

        List<ImageResource> images = aiGeneratorImageTool.aiGeneratorImage(type, description);

        log.info("========================================");
        log.info("生成了 {} 张图片", images.size());
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
     * 测试插图生成
     */
    @Test
    public void testIllustrationGeneration() {
        log.info("========================================");
        log.info("开始测试：插图生成");
        log.info("========================================");

        String type = "插图";
        String description = "学习场景插图，扁平化风格，温暖色调，展示学生使用电脑学习编程";

        log.info("生成类型: {}", type);
        log.info("描述信息: {}", description);

        List<ImageResource> images = aiGeneratorImageTool.aiGeneratorImage(type, description);

        log.info("========================================");
        log.info("生成了 {} 张图片", images.size());
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
     * 测试前端学习产物场景（模拟工作流）
     */
    @Test
    public void testFrontendLearningScenario() {
        log.info("========================================");
        log.info("开始测试：前端学习产物场景");
        log.info("========================================");

        String type = "概念视觉图";
        String description = "前端知识学习单页产物，卡片式布局展示HTML、CSS、JavaScript示例，代码高亮，实时预览功能，现代化UI设计";

        log.info("生成类型: {}", type);
        log.info("描述信息: {}", description);

        List<ImageResource> images = aiGeneratorImageTool.aiGeneratorImage(type, description);

        log.info("========================================");
        log.info("生成了 {} 张图片", images.size());
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

        log.info("工具名称: {}", aiGeneratorImageTool.getToolName());
        log.info("工具显示名称: {}", aiGeneratorImageTool.getToolDisplayName());

        log.info("========================================");
    }

    /**
     * 测试异常处理（空描述）
     */
    @Test
    public void testEmptyDescription() {
        log.info("========================================");
        log.info("开始测试：空描述异常处理");
        log.info("========================================");

        String type = "Logo";
        String description = "";

        log.info("生成类型: {}", type);
        log.info("描述信息: {}", description);

        List<ImageResource> images = aiGeneratorImageTool.aiGeneratorImage(type, description);

        log.info("========================================");
        log.info("生成了 {} 张图片（预期为0或处理异常）", images.size());
        log.info("========================================");
    }
}
