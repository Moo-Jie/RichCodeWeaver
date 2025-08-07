package com.rich.richcodeweaver.utiles.ai;

import com.rich.richcodeweaver.model.enums.CodeGeneratorTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

@SpringBootTest
class AIGenerateCodeAndSaveToFileUtilsTest {

    @Resource
    private AIGenerateCodeAndSaveToFileUtils aiGenerateCodeAndSaveToFileUtils;

    /**
     * 测试生成 HTML 代码并保存至本地（非流式）
     *
     * @return void
     * @author DuRuiChi
     * @create 2025/8/6
     **/
    @Test
    void aiGenerateAndSaveCode() {
        File htmlFile = aiGenerateCodeAndSaveToFileUtils.aiGenerateAndSaveCode("为我生成一个天气预报网站", CodeGeneratorTypeEnum.HTML);
        Assertions.assertNotNull(htmlFile);
    }

    /**
     * 测试生成多文件代码并保存至本地（非流式）
     *
     * @return void
     * @author DuRuiChi
     * @create 2025/8/6
     **/
    @Test
    void aiGenerateAndSaveMultiFileCode() {
        File multiFileDir = aiGenerateCodeAndSaveToFileUtils.aiGenerateAndSaveCode("为我生成一个天气预报网站", CodeGeneratorTypeEnum.MULTI_FILE);
        Assertions.assertNotNull(multiFileDir);
    }

    /**
     * 测试生成 HTML 代码并保存至本地（流式）
     *
     * @return void
     * @author DuRuiChi
     * @create 2025/8/6
     **/
    @Test
    void generateAndSaveCodeStream() {
        Flux<String> codeStream = aiGenerateCodeAndSaveToFileUtils.aiGenerateAndSaveCodeStream("为我生成一个自律打卡网站网站", CodeGeneratorTypeEnum.HTML);
        // 收集流式对象
        List<String> result = codeStream.collectList().block();
        Assertions.assertNotNull(result);
        // 拼接流式对象
        String completeContent = String.join("", result);
        Assertions.assertNotNull(completeContent);
    }

    /**
     * 测试生成多文件代码并保存至本地（流式）
     *
     * @return void
     * @author DuRuiChi
     * @create 2025/8/6
     **/
    @Test
    void generateAndSaveCodeMultiFileStream() {
        Flux<String> codeStream = aiGenerateCodeAndSaveToFileUtils.aiGenerateAndSaveCodeStream("为我生成一个自律打卡网站网站", CodeGeneratorTypeEnum.MULTI_FILE);
        // 收集流式对象
        List<String> result = codeStream.collectList().block();
        Assertions.assertNotNull(result);
        // 拼接流式对象
        String completeContent = String.join("", result);
        Assertions.assertNotNull(completeContent);
    }

}