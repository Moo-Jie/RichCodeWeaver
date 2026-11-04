package com.rich.app.utils;

import com.rich.ai.service.AiCodeGeneratorService;
import com.rich.app.factory.AiCodeGenWorkflowServiceFactory;
import com.rich.app.utils.ConvertTokenStreamToFluxUtils.ConvertWorkflowTokenStreamToFluxUtils;
import com.rich.app.utils.codeParse.CodeParseExecutor;
import com.rich.app.utils.codeSave.CodeResultSaveExecutor;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.common.utils.SpringContextUtil;
import com.rich.model.enums.CodeGeneratorTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * AI 生成代码并保存为本地文件
 * （门面设计模式：整合 AI 代码生成器服务 和 本地保存工具包，流式整合代码解析为封装类）
 *
 * @author DuRuiChi
 * @create 2025/12/5
 **/
@Slf4j
@Service
public class AIGenerateCodeAndSaveToFileUtils {

    /**
     * 生成类型为空提示
     */
    private static final String EMPTY_GENERATOR_TYPE_MESSAGE = "生成类型为空";

    /**
     * 不支持的生成类型提示模板
     */
    private static final String UNSUPPORTED_GENERATOR_TYPE_TEMPLATE = "不支持的生成类型：%s";

    /**
     * 代码生成失败提示前缀
     */
    private static final String CODE_GENERATION_FAILED_PREFIX = "代码生成失败: ";

    @Resource
    private ConvertWorkflowTokenStreamToFluxUtils convertWorkflowTokenStreamToFluxUtils;

    /**
     * 通过判断代码生成业务类型，调用对应的 AI 服务生成代码流，并保存到本地（流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 代码生成类型
     * @param appId           产物id
     * @return reactor.core.publisher.Flux<java.lang.String>    代码流
     * @author DuRuiChi
     * @create 2025/12/6
     **/
    public Flux<String> aiGenerateAndSaveCodeStream(String userMessage, CodeGeneratorTypeEnum codeGenTypeEnum, Long appId) {
        try {
            // 1：校验代码生成类型是否为空
            if (codeGenTypeEnum == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, EMPTY_GENERATOR_TYPE_MESSAGE);
            }

            log.info("（流式）开始生成{}类型代码，用户需求：{}", codeGenTypeEnum.getValue(), userMessage);

            // 2：从 Spring 容器中获取 AI 代码生成器工厂
            AiCodeGeneratorService aiCodeGeneratorService = getAiCodeGeneratorService(appId, codeGenTypeEnum);

            // 4：根据代码生成类型选择对应的生成策略
            return switch (codeGenTypeEnum) {
                // HTML 单文件模式：生成 HTML 代码流
                // langchain4j 不支持流式输出格式化，故自定义解析逻辑
                case HTML, MULTI_FILE -> {
                    Flux<String> resultStream = generateCodeStream(aiCodeGeneratorService, userMessage, codeGenTypeEnum, appId);
                    // 解析并保存代码流
                    yield parseAndSaveCodeStream(resultStream, codeGenTypeEnum, appId);
                }
                // Vue 项目模式：生成 Vue 项目代码流（使用推理模型）
                case VUE_PROJECT -> generateCodeStream(aiCodeGeneratorService, userMessage, codeGenTypeEnum, appId);
                // 默认情况：不支持的类型，抛出异常
                default -> {
                    String errorMessage = String.format(UNSUPPORTED_GENERATOR_TYPE_TEMPLATE, codeGenTypeEnum.getValue());
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
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, CODE_GENERATION_FAILED_PREFIX + e.getMessage());
        }
    }

    /**
     * 获取 AI 代码生成服务实例
     *
     * @param appId 产物 ID
     * @param codeGenTypeEnum 代码生成类型
     * @return AI 代码生成服务实例
     */
    private AiCodeGeneratorService getAiCodeGeneratorService(Long appId, CodeGeneratorTypeEnum codeGenTypeEnum) {
        AiCodeGenWorkflowServiceFactory aiCodeGenWorkflowServiceFactory =
                SpringContextUtil.getBean(AiCodeGenWorkflowServiceFactory.class);
        return aiCodeGenWorkflowServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
    }

    /**
     * 根据生成类型获取对应的代码流
     *
     * @param aiCodeGeneratorService AI 代码生成服务
     * @param userMessage 用户提示词
     * @param codeGenTypeEnum 代码生成类型
     * @param appId 产物 ID
     * @return 代码流
     */
    private Flux<String> generateCodeStream(AiCodeGeneratorService aiCodeGeneratorService, String userMessage,
                                            CodeGeneratorTypeEnum codeGenTypeEnum, Long appId) {
        return switch (codeGenTypeEnum) {
            case HTML -> aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
            case MULTI_FILE -> aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
            case VUE_PROJECT -> convertWorkflowTokenStreamToFluxUtils.convertTokenStreamToFlux(
                    aiCodeGeneratorService.generateVueProjectCodeStream(userMessage, appId), appId);
            default -> throw new BusinessException(
                    ErrorCode.SYSTEM_ERROR,
                    String.format(UNSUPPORTED_GENERATOR_TYPE_TEMPLATE, codeGenTypeEnum.getValue()));
        };
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
     * @create 2025/12/6
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
                    // 1：将拼接好的代码字符串解析为代码封装类
                    Object codeResult = CodeParseExecutor.executeParseCode(
                            codeBuilder.toString(), codeGeneratorTypeEnum);
                    // 2：将代码封装类保存为本地文件
                    CodeResultSaveExecutor.executeSaver(codeResult, codeGeneratorTypeEnum, appId);
                });
    }
}
