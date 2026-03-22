package com.rich.app.utils;

import com.rich.ai.model.codeResponse.HtmlCodeResponse;
import com.rich.ai.model.codeResponse.MultiFileCodeResponse;
import com.rich.ai.service.AiCodeGeneratorService;
import com.rich.app.factory.AiCodeGeneratorServiceFactory;
import com.rich.app.utils.codeParse.CodeParseExecutor;
import com.rich.app.utils.codeSave.CodeResultSaveExecutor;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.common.utils.SpringContextUtil;
import com.rich.model.enums.CodeGeneratorTypeEnum;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

import static com.rich.model.enums.CodeGeneratorTypeEnum.MULTI_FILE;

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
     * 通过判断代码生成业务类型，调用对应的 AI 服务生成代码流，并保存到本地（流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 代码生成类型
     * @param appId           产物id
     * @return reactor.core.publisher.Flux<java.lang.String>    代码流
     * @author DuRuiChi
     * @create 2025/8/6
     **/
    public Flux<String> aiGenerateAndSaveCodeStream(String userMessage, CodeGeneratorTypeEnum codeGenTypeEnum, Long appId) {
        try {
            // 步骤1：校验代码生成类型是否为空
            if (codeGenTypeEnum == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
            }

            log.info("（流式）开始生成{}类型代码，用户需求：{}", codeGenTypeEnum.getValue(), userMessage);

            // 步骤2：从 Spring 容器中获取 AI 代码生成器工厂
            AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory = 
                    SpringContextUtil.getBean(AiCodeGeneratorServiceFactory.class);

            // 步骤3：通过工厂获取或创建 AI 服务实例（带缓存）
            AiCodeGeneratorService aiCodeGeneratorService = 
                    aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);

            // 步骤4：根据代码生成类型选择对应的生成策略
            return switch (codeGenTypeEnum) {
                // HTML 单文件模式：生成 HTML 代码流
                // langchain4j 不支持流式输出格式化，故自定义解析逻辑
                case HTML -> {
                    Flux<String> resultStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                    // 解析并保存代码流
                    yield parseAndSaveCodeStream(resultStream, codeGenTypeEnum, appId);
                }
                // 多文件模式：生成多文件代码流
                case MULTI_FILE -> {
                    Flux<String> resultStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                    // 解析并保存代码流
                    yield parseAndSaveCodeStream(resultStream, codeGenTypeEnum, appId);
                }
                // Vue 项目模式：生成 Vue 项目代码流（使用推理模型）
                case VUE_PROJECT -> {
                    TokenStream resultStream = aiCodeGeneratorService.generateVueProjectCodeStream(userMessage, appId);
                    // 将 LangChain4j 的 TokenStream 转换为 Reactor 的 Flux<String>
                    yield convertTokenStreamToFluxUtils.convertTokenStreamToFlux(resultStream, appId);
                }
                // 默认情况：不支持的类型，抛出异常
                default -> {
                    String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
                }
            };
        } catch (BusinessException e) {
            // 捕获业务异常，记录日志并重新抛出
            log.error("（流式）AI 生成代码并保存至本地业务异常：{}，错误码：{}", e.getMessage(), e.getCode());
            throw e;
        } catch (Exception e) {
            // 捕获系统异常，记录详细日志并封装为业务异常
            log.error("（流式）AI 生成代码系统异常：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码生成失败: " + e.getMessage());
        }
    }

    /**
     * 解析代码流，根据代码类型调用对应的解析器，解析代码块
     * （ langchain4j 不支持流式输出格式化，故自定义相关解析逻辑）
     *
     * @param resultStream          代码流
     * @param codeGeneratorTypeEnum 代码类型
     * @param appId                 产物id
     * @return reactor.core.publisher.Flux<java.lang.String>    代码流
     * @author DuRuiChi
     * @create 2025/8/6
     **/
    private Flux<String> parseAndSaveCodeStream(Flux<String> resultStream, CodeGeneratorTypeEnum codeGeneratorTypeEnum, Long appId) {
        // 创建 StringBuilder 用于拼接代码流中的所有代码块
        StringBuilder codeBuilder = new StringBuilder();
        
        // 处理代码流：每次收到代码块时追加到 StringBuilder，流结束时解析并保存
        return resultStream
                // doOnNext：每次收到代码块时追加到 codeBuilder
                .doOnNext(codeBuilder::append)
                // doOnComplete：流结束时执行解析和保存逻辑
                .doOnComplete(() -> {
                    // 步骤1：将拼接好的代码字符串解析为代码封装类
                    Object codeResult = CodeParseExecutor.executeParseCode(
                            codeBuilder.toString(), codeGeneratorTypeEnum);
                    // 步骤2：将代码封装类保存为本地文件
                    CodeResultSaveExecutor.executeSaver(codeResult, codeGeneratorTypeEnum, appId);
                });
    }
}
