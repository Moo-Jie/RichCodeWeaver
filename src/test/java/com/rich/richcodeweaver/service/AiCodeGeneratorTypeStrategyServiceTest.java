package com.rich.richcodeweaver.service;

import com.rich.richcodeweaver.model.enums.CodeGeneratorTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class AiCodeGeneratorTypeStrategyServiceTest {

    @Resource
    private AiCodeGeneratorTypeStrategyService aiCodeGeneratorTypeStrategyService;

    @Test
    void getCodeGenStrategy() {
        CodeGeneratorTypeEnum codeGenStrategy01 = aiCodeGeneratorTypeStrategyService.getCodeGenStrategy("为我生成一个简单的表白墙。");
        log.info("为我生成一个简单的表白墙。   |   代码生成策略: {}", codeGenStrategy01);
        CodeGeneratorTypeEnum codeGenStrategy02 = aiCodeGeneratorTypeStrategyService.getCodeGenStrategy("为我生成有多个页面的表白墙。");
        log.info("为我生成有多个页面的表白墙。  |   代码生成策略: {}", codeGenStrategy02);
        CodeGeneratorTypeEnum codeGenStrategy03 = aiCodeGeneratorTypeStrategyService.getCodeGenStrategy("为我生成一个前端知识平台，可以包含各种各样的前端知识展示。");
        log.info("为我生成一个前端知识平台，可以包含各种各样的前端知识展示。  |   代码生成策略: {}", codeGenStrategy03);
    }
}