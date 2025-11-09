package com.rich.richcodeweaver.utils.aiUtils;

import com.rich.richcodeweaver.exception.BusinessException;
import com.rich.richcodeweaver.exception.ErrorCode;
import com.rich.richcodeweaver.factory.AiCodeGeneratorServiceFactory;
import com.rich.richcodeweaver.model.aiChatResponse.codeResponse.HtmlCodeResponse;
import com.rich.richcodeweaver.model.aiChatResponse.codeResponse.MultiFileCodeResponse;
import com.rich.richcodeweaver.model.enums.CodeGeneratorTypeEnum;
import com.rich.richcodeweaver.service.aiChatService.AiCodeGeneratorService;
import com.rich.richcodeweaver.utils.SpringContextUtil;
import com.rich.richcodeweaver.utils.aiUtils.codeParse.CodeParseExecutor;
import com.rich.richcodeweaver.utils.aiUtils.codeSave.CodeResultSaveExecutor;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

import static com.rich.richcodeweaver.model.enums.CodeGeneratorTypeEnum.MULTI_FILE;

/**
 * AI 生成代码并保存为本地文件
 * （门面设计模式：整合 AI 代码生成器服务 和 本地保存工具包，流式整合代码解析为封装类）
 *
 * @author DuRuiChi
 * @create 2025/8/5
 **/
@Slf4j
@Service
public class AIGenerateCodeAndSaveToFileUtils {
    @Resource
    private ConvertTokenStreamToFluxUtils convertTokenStreamToFluxUtils;

    /**
     * 通过判断代码生成业务类型，调用对应的 AI 服务生成代码，并保存到本地（非流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 代码生成类型
     * @param appId           应用id
     * @return java.io.File
     * @author DuRuiChi
     * @create 2025/8/5
     **/
    public File aiGenerateAndSaveCode(String userMessage, CodeGeneratorTypeEnum codeGenTypeEnum, Long appId) {
        try {
            if (codeGenTypeEnum == null) {
                log.error("代码生成类型为空，用户输入：{}", userMessage);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
            }

            log.info("（非流式）开始生成{}类型代码，用户需求：{}", codeGenTypeEnum.getValue(), userMessage);

            // 引入 AI 代码生成器并发服务
            AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory = SpringContextUtil.getBean(AiCodeGeneratorServiceFactory.class);

            // 自定义 AI 服务工厂，用于通过 AppId 创建 AI 服务实例
            AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);

            return switch (codeGenTypeEnum) {
                case HTML -> {
                    HtmlCodeResponse result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                    yield CodeResultSaveExecutor.executeSaver(result, codeGenTypeEnum, appId);
                }
                case MULTI_FILE -> {
                    MultiFileCodeResponse result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                    yield CodeResultSaveExecutor.executeSaver(result, codeGenTypeEnum, appId);
                }
                case VUE_PROJECT -> {
                    MultiFileCodeResponse result = aiCodeGeneratorService.generateVueProjectCode(userMessage, appId);
                    yield CodeResultSaveExecutor.executeSaver(result, MULTI_FILE, appId);
                }
                default -> {
                    String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                    log.error(errorMessage);
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
                }
            };
        } catch (BusinessException e) {
            log.error("（非流式）AI 生成代码并保存至本地业务异常：{}，错误码：{}", e.getMessage(), e.getCode());
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
     * @param appId           应用id
     * @return reactor.core.publisher.Flux<java.lang.String>    代码流
     * @author DuRuiChi
     * @create 2025/8/6
     **/
    public Flux<String> aiGenerateAndSaveCodeStream(String userMessage, CodeGeneratorTypeEnum codeGenTypeEnum, Long appId) {
        try {
            if (codeGenTypeEnum == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
            }

            log.info("（流式）开始生成{}类型代码，用户需求：{}", codeGenTypeEnum.getValue(), userMessage);

            // 引入 AI 代码生成器并发服务
            AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory = SpringContextUtil.getBean(AiCodeGeneratorServiceFactory.class);

            // AI 服务工厂，用于通过 AppId 创建 AI 服务实例
            AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);

            return switch (codeGenTypeEnum) {
                // langchain4j 不支持流式输出格式化，故自定义相关解析逻辑
                case HTML -> {
                    Flux<String> resultStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                    yield parseAndSaveCodeStream(resultStream, codeGenTypeEnum, appId);
                }
                case MULTI_FILE -> {
                    Flux<String> resultStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                    yield parseAndSaveCodeStream(resultStream, codeGenTypeEnum, appId);
                }
                case VUE_PROJECT -> {
                    TokenStream resultStream = aiCodeGeneratorService.generateVueProjectCodeStream(userMessage, appId);
                    // 将 LangChain4j 的 TokenStream 转换为 Reactor 的 Flux<String>
                    yield convertTokenStreamToFluxUtils.convertTokenStreamToFlux(resultStream, appId);
                }
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
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码生成失败:" + e.getMessage());
        }
    }

    /**
     * 解析代码流，根据代码类型调用对应的解析器，解析代码块
     * （ langchain4j 不支持流式输出格式化，故自定义相关解析逻辑）
     *
     * @param resultStream          代码流
     * @param codeGeneratorTypeEnum 代码类型
     * @param appId                 应用id
     * @return reactor.core.publisher.Flux<java.lang.String>    代码流
     * @author DuRuiChi
     * @create 2025/8/6
     **/
    private Flux<String> parseAndSaveCodeStream(Flux<String> resultStream, CodeGeneratorTypeEnum codeGeneratorTypeEnum, Long appId) {
        // 拼接代码流内的代码块
        StringBuilder codeBuilder = new StringBuilder();
        // 代码块拼接完成后，解析代码块
        return resultStream.doOnNext(codeBuilder::append).doOnComplete(() -> {
            // 代码字符串解析为代码封装类执行器
            Object codeResult = CodeParseExecutor.executeParseCode(codeBuilder.toString(), codeGeneratorTypeEnum);
            // 代码封装类保存文件执行器
            CodeResultSaveExecutor.executeSaver(codeResult, codeGeneratorTypeEnum, appId);
        });
    }
}
