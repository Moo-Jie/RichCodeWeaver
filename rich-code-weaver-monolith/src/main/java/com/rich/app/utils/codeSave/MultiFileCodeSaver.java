package com.rich.app.utils.codeSave;

import cn.hutool.core.util.StrUtil;
import com.rich.ai.model.codeResponse.MultiFileCodeResponse;
import com.rich.model.enums.CodeGeneratorTypeEnum;

/**
 * 多文件代码文件保存器
 *
 * @author DuRuiChi
 * @create 2025/12/10
 **/
public class MultiFileCodeSaver extends CodeResultSaveToFileTemplate<MultiFileCodeResponse> {

    @Override
    protected String getCodeGeneratorTypeEnumValue() {
        return CodeGeneratorTypeEnum.MULTI_FILE.getValue();
    }

    @Override
    protected void doSaveCodeResult(MultiFileCodeResponse result, String baseDirPath) {
        // 重写保存逻辑，直接调用单文件写入模板方法
        // 写入文件
        writeSingleToFile(baseDirPath, "index.html", StrUtil.nullToEmpty(result.getHtmlCode()));
        writeSingleToFile(baseDirPath, "style.css", StrUtil.nullToEmpty(result.getCssCode()));
        writeSingleToFile(baseDirPath, "script.js", StrUtil.nullToEmpty(result.getJsCode()));
    }
}
