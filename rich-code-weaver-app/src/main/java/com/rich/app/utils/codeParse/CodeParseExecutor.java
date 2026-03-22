package com.rich.app.utils.codeParse;

import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.model.enums.CodeGeneratorTypeEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * 代码字符串解析为代码封装类执行器
 * （执行器模式：执行不同策略模式下的代码字符串解析为代码封装操作）
 *
 * @author DuRuiChi
 * @create 2025/8/7
 **/


@Slf4j
public class CodeParseExecutor {

    /**
     * Html 单文件策略模式
     */
    private static final AiResToHtmlCodeResultParser aiResToHtmlCodeResultParser = new AiResToHtmlCodeResultParser();

    /**
     * 多文件策略模式
     */
    private static final AiResToMultiFileCodeResultParser aiResToMultiFileCodeResultParser = new AiResToMultiFileCodeResultParser();

    /**
     * 分策略执行代码解析
     *
     * @param codeContent           代码内容
     * @param codeGeneratorTypeEnum 代码生成类型
     * @return 解析结果（HtmlCodeResponse 或 MultiFileCodeResponse）
     */
    public static Object executeParseCode(String codeContent, CodeGeneratorTypeEnum codeGeneratorTypeEnum) {
        try {
            // 步骤1：校验代码生成类型是否为空
            if (codeGeneratorTypeEnum == null) {
                log.error("代码生成类型为空，用户输入：{}", codeContent);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
            }

            log.info("开始解析{}类型代码，代码内容长度：{}", 
                    codeGeneratorTypeEnum.getValue(), 
                    codeContent != null ? codeContent.length() : 0);

            // 步骤2：根据代码生成类型选择对应的解析策略
            return switch (codeGeneratorTypeEnum) {
                // 单文件 HTML 模式：使用 HTML 代码解析器
                case HTML -> aiResToHtmlCodeResultParser.parseCode(codeContent);
                // 多文件模式 / Vue 项目模式：均使用多文件解析器
                case MULTI_FILE, VUE_PROJECT -> aiResToMultiFileCodeResultParser.parseCode(codeContent);
                // 默认情况：不支持的类型，抛出异常
                default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, 
                        "不支持的代码解析类型: " + codeGeneratorTypeEnum.getValue());
            };
        } catch (BusinessException e) {
            // 捕获业务异常，记录日志并重新抛出
            log.error("代码解析业务异常：{}，错误码：{}", e.getMessage(), e.getCode());
            throw e;
        } catch (Exception e) {
            // 捕获系统异常，记录详细日志并封装为业务异常
            log.error("代码解析系统异常：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码解析失败： " + e.getMessage());
        }
    }
}
