package com.rich.richcodeweaver.utils.aiUtils.codeParse;

import com.rich.richcodeweaver.exception.BusinessException;
import com.rich.richcodeweaver.exception.ErrorCode;
import com.rich.richcodeweaver.model.enums.CodeGeneratorTypeEnum;
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
            // 校验代码生成类型
            if (codeGeneratorTypeEnum == null) {
                log.error("代码生成类型为空，用户输入：{}", codeContent);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
            }

            log.info("开始生成{}类型代码，用户需求：{}", codeGeneratorTypeEnum.getValue(), codeContent);

            // 执行不同策略模式下的代码解析
            return switch (codeGeneratorTypeEnum) {
                // 单文件 HTML 模式
                case HTML -> aiResToHtmlCodeResultParser.parseCode(codeContent);
                // 多文件模式
                case MULTI_FILE -> aiResToMultiFileCodeResultParser.parseCode(codeContent);
                // TODO 其他策略模式
                default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码解析类型: " + codeContent);
            };
        } catch (BusinessException e) {
            log.error("业务异常：{}，错误码：{}", e.getMessage(), e.getCode());
            throw e;
        } catch (Exception e) {
            log.error("系统异常：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码解析失败");
        }
    }
}
