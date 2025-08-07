package com.rich.richcodeweaver.utiles.ai;

import com.rich.richcodeweaver.exception.BusinessException;
import com.rich.richcodeweaver.exception.ErrorCode;
import com.rich.richcodeweaver.model.dto.ai.HtmlCodeResult;
import com.rich.richcodeweaver.model.dto.ai.MultiFileCodeResult;
import com.rich.richcodeweaver.model.enums.CodeGeneratorTypeEnum;
import com.rich.richcodeweaver.service.AiCodeGeneratorService;
import com.rich.richcodeweaver.utiles.CodeSaveToFileUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 生成代码并保存为本地文件
 * （整合 AI 代码生成器服务 和 本地保存工具包，流式整合代码解析为封装类）
 *
 * @author DuRuiChi
 * @create 2025/8/5
 **/
@Slf4j
@Service
public class AIGenerateCodeAndSaveToFileUtils {

    /**
     * 引入 AI 代码生成器服务
     **/
    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;


    /**
     * 通过判断代码生成业务类型，调用对应的 AI 服务生成代码，并保存到本地（非流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 代码生成类型
     * @return java.io.File
     * @author DuRuiChi
     * @create 2025/8/5
     **/
    public File aiGenerateAndSaveCode(String userMessage, CodeGeneratorTypeEnum codeGenTypeEnum) {
        try {
            if (codeGenTypeEnum == null) {
                log.error("代码生成类型为空，用户输入：{}", userMessage);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
            }

            log.info("开始生成{}类型代码，用户需求：{}", codeGenTypeEnum.getValue(), userMessage);

            return switch (codeGenTypeEnum) {
                case HTML -> generateAndSaveHtmlCode(userMessage);
                case MULTI_FILE -> generateAndSaveMultiFileCode(userMessage);
                default -> {
                    String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                    log.error(errorMessage);
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
                }
            };
        } catch (BusinessException e) {
            log.error("业务异常：{}，错误码：{}", e.getMessage(), e.getCode());
            throw e;
        } catch (Exception e) {
            log.error("系统异常：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码生成失败");
        }
    }

    /**
     * 通过判断代码生成业务类型，调用对应的 AI 服务生成代码流，并保存到本地（流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 代码生成类型
     * @return reactor.core.publisher.Flux<java.lang.String>    代码流
     * @author DuRuiChi
     * @create 2025/8/6
     **/
    public Flux<String> aiGenerateAndSaveCodeStream(String userMessage, CodeGeneratorTypeEnum codeGenTypeEnum) {
        try {
            if (codeGenTypeEnum == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
            }

            log.info("开始生成{}类型代码，用户需求：{}", codeGenTypeEnum.getValue(), userMessage);

            return switch (codeGenTypeEnum) {
                case HTML -> generateAndSaveHtmlCodeStream(userMessage);
                case MULTI_FILE -> generateAndSaveMultiFileCodeStream(userMessage);
                default -> {
                    String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
                }
            };
        } catch (BusinessException e) {
            log.error("（流式）AI 生成代码并保存至本地业务异常：{}，错误码：{}", e.getMessage(), e.getCode());
            throw e;
        } catch (Exception e) {
            log.error("系统异常：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码生成失败");
        }
    }

    /**
     * 生成多文件模式下的流式代码，解析为多文件封装类，最后保存至本地（流式）
     *
     * @param userMessage 用户提示词
     * @return reactor.core.publisher.Flux<java.lang.String>    代码流
     * @author DuRuiChi
     * @create 2025/8/6
     **/
    private Flux<String> generateAndSaveMultiFileCodeStream(String userMessage) {
        // 获取 AI 响应流
        Flux<String> resultStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
        // 拼接代码流内的代码块
        StringBuilder codeBuilder = new StringBuilder();
        // 代码块拼接完成后，解析代码块
        return resultStream.doOnNext(codeBuilder::append).doOnComplete(() -> {
            // 解析代码块
            MultiFileCodeResult multiFileCodeResult = CodeParseUtils.parseMultiFileCode(codeBuilder.toString());
            // 保存代码
            CodeSaveToFileUtils.saveMultiFileCodeResult(multiFileCodeResult);
        });
    }

    /**
     * 生成 HTML 模式下的流式代码，解析为 HTML 封装类，最后保存至本地（流式）
     *
     * @param userMessage 用户提示词
     * @return reactor.core.publisher.Flux<java.lang.String>    代码流
     * @author DuRuiChi
     * @create 2025/8/6
     **/
    private Flux<String> generateAndSaveHtmlCodeStream(String userMessage) {
        // 获取 AI 响应流
        Flux<String> resultStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
        // 拼接代码流内的代码块
        StringBuilder codeBuilder = new StringBuilder();
        // 代码块拼接完成后，解析代码块
        return resultStream.doOnNext(codeBuilder::append).doOnComplete(() -> {
            // 解析代码块
            HtmlCodeResult htmlCodeResult = CodeParseUtils.parseHtmlCode(codeBuilder.toString());
            // 保存代码
            CodeSaveToFileUtils.saveHtmlCodeResult(htmlCodeResult);
        });
    }

    /**
     * 生成 HTML 模式下的代码并保存至本地（非流式）
     *
     * @param userMessage 用户提示词
     * @return java.io.File    保存的文件
     * @author DuRuiChi
     * @create 2025/8/5
     **/
    private File generateAndSaveHtmlCode(String userMessage) {
        try {
            HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
            log.info("成功生成HTML代码，保存路径：{}", CodeSaveToFileUtils.FILE_SAVE_ROOT_DIR);
            return CodeSaveToFileUtils.saveHtmlCodeResult(result);
        } catch (Exception e) {
            log.error("HTML代码生成失败：{}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码生成失败");
        }
    }

    /**
     * 生成多文件模式下的代码并保存至本地（非流式）
     *
     * @param userMessage 用户提示词
     * @return java.io.File    保存的目录
     * @author DuRuiChi
     * @create 2025/8/5
     **/
    private File generateAndSaveMultiFileCode(String userMessage) {
        try {
            MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
            log.info("成功生成多文件代码，保存路径：{}", CodeSaveToFileUtils.FILE_SAVE_ROOT_DIR);
            return CodeSaveToFileUtils.saveMultiFileCodeResult(result);
        } catch (Exception e) {
            log.error("多文件代码生成失败：{}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "多文件代码生成失败");
        }
    }
}
