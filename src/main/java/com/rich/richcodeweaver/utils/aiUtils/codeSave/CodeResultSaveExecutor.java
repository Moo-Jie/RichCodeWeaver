package com.rich.richcodeweaver.utils.aiUtils.codeSave;

import com.rich.richcodeweaver.exception.BusinessException;
import com.rich.richcodeweaver.exception.ErrorCode;
import com.rich.richcodeweaver.model.aiChatResponse.codeResponse.HtmlCodeResponse;
import com.rich.richcodeweaver.model.aiChatResponse.codeResponse.MultiFileCodeResponse;
import com.rich.richcodeweaver.model.enums.CodeGeneratorTypeEnum;

import java.io.File;

/**
 * 代码封装类保存文件执行器
 * （执行器模式：执行不同策略模式下的代码封装类的保存文件操作）
 *
 * @author DuRuiChi
 * @create 2025/8/7
 **/
public class CodeResultSaveExecutor {

    private static final HtmlCodeSaver htmlCodeFileSaver = new HtmlCodeSaver();

    private static final multiFileCodeSaver multiFileCodeFileSaver = new multiFileCodeSaver();

    /**
     * 执行代码保存
     *
     * @param codeResult  代码结果对象
     * @param codeGenType 代码生成类型
     * @param appId       应用id
     * @return 保存的目录
     */
    public static File executeSaver(Object codeResult, CodeGeneratorTypeEnum codeGenType, Long appId) {
        return switch (codeGenType) {
            case HTML -> htmlCodeFileSaver.saveCodeResult((HtmlCodeResponse) codeResult, appId);
            case MULTI_FILE -> multiFileCodeFileSaver.saveCodeResult((MultiFileCodeResponse) codeResult, appId);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型: " + codeGenType);
        };
    }
}
