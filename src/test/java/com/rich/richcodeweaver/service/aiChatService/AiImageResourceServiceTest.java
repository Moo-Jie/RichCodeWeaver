package com.rich.richcodeweaver.service.aiChatService;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AiImageResourceServiceTest {

    @Resource
    private AiImageResourceService aiImageResourceService;

    @Test
    void testResourceImages() {
        String result = aiImageResourceService.resourceImages("创建一个前端技术网站，展示相关 JS、CSS、TS、SCSS等语法");
        Assertions.assertNotNull(result);
        System.out.println("技术网站收集到的图片: \n" + result);
    }
}