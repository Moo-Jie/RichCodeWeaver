package com.rich.app.utils.codeSave;

import com.rich.ai.model.codeResponse.HtmlCodeResponse;
import com.rich.model.enums.CodeGeneratorTypeEnum;

/**
 * HTML代码文件保存器
 *
 * @author DuRuiChi
 * @create 2025/8/10
 **/
public class HtmlCodeSaver extends CodeResultSaveToFileTemplate<HtmlCodeResponse> {

    @Override
    protected String getCodeGeneratorTypeEnumValue() {
        // 返回 HTML 代码生成类型的枚举值，用于构建保存目录路径
        return CodeGeneratorTypeEnum.HTML.getValue();
    }

    @Override
    protected void doSaveCodeResult(HtmlCodeResponse result, String baseDirPath) {
        // 将 HTML 代码保存为单个 index.html 文件
        // baseDirPath: 基础目录路径
        // "index.html": 文件名
        // result.getHtmlCode(): HTML 代码内容
        writeSingleToFile(baseDirPath, "index.html", result.getHtmlCode());
    }
}
