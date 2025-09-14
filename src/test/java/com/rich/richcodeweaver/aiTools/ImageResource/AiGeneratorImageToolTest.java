package com.rich.richcodeweaver.aiTools.ImageResource;

import com.rich.richcodeweaver.model.entity.ImageResource;
import com.rich.richcodeweaver.model.enums.ImageCategoryEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class AiGeneratorImageToolTest {

    @Resource
    private AiGeneratorImageTool aiGeneratorImageTool;

    @Test
    void testAiGeneratorImage() {
        // 测试正常搜索
        List<ImageResource> image = aiGeneratorImageTool.aiGeneratorImage("风景", "雪山上的飞燕");
        assertNotNull(image);
        ImageResource firstLogo = image.getFirst();
        assertEquals(ImageCategoryEnum.AI, firstLogo.getCategory());
        assertNotNull(firstLogo.getDescription());
        assertNotNull(firstLogo.getUrl());
        image.forEach(i ->
                System.out.println("生成结果: " + i.getDescription() + " - " + i.getUrl())
        );
    }
}