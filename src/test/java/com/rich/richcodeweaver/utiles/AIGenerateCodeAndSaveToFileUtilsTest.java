package com.rich.richcodeweaver.utiles;

import com.rich.richcodeweaver.model.enums.CodeGeneratorTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AIGenerateCodeAndSaveToFileUtilsTest {
    @Resource
    private AIGenerateCodeAndSaveToFileUtils aiGenerateCodeAndSaveToFileUtils;

    @Test
    void aiGenerateAndSaveCode() {
//        File htmlFile = aiGenerateCodeAndSaveToFileUtils.aiGenerateAndSaveCode("为我生成一个自律打卡网站网站", CodeGeneratorTypeEnum.HTML);
        File htmlFile = aiGenerateCodeAndSaveToFileUtils.aiGenerateAndSaveCode("为我生成一个自律打卡网站网站", CodeGeneratorTypeEnum.MULTI_FILE);
        Assertions.assertNotNull(htmlFile);
    }

    @Test
    void aiGenerateAndSaveCodeStream() {
//        Flux<String> codeStream = aiGenerateCodeAndSaveToFileUtils.aiGenerateAndSaveCodeStream("为我生成一个自律打卡网站网站", CodeGeneratorTypeEnum.HTML);
        Flux<String> codeStream = aiGenerateCodeAndSaveToFileUtils.aiGenerateAndSaveCodeStream("为我生成一个自律打卡网站网站", CodeGeneratorTypeEnum.MULTI_FILE);

        // 收集流式对象
        List<String> result = codeStream.collectList().block();
        Assertions.assertNotNull(result);
        // 拼接流式对象
        String completeContent = String.join("", result);
        Assertions.assertNotNull(completeContent);
    }
}