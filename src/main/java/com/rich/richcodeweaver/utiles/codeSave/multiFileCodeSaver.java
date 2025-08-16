package com.rich.richcodeweaver.utiles.codeSave;

import com.rich.richcodeweaver.model.dto.aiCode.MultiFileCodeResult;
import com.rich.richcodeweaver.model.enums.CodeGeneratorTypeEnum;

/**
 * 多文件代码文件保存器
 *
 * @author DuRuiChi
 * @create 2025/8/10
 **/
public class multiFileCodeSaver extends CodeResultSaveToFileTemplate<MultiFileCodeResult> {

    @Override
    protected String getCodeGeneratorTypeEnumValue() {
        return CodeGeneratorTypeEnum.MULTI_FILE.getValue();
    }

    @Override
    protected void doSaveCodeResult(MultiFileCodeResult result, String baseDirPath) {
        // 重写保存逻辑，直接调用单文件写入模板方法
        // 写入文件
        writeSingleToFile(baseDirPath, "index.html", result.getHtmlCode());
        writeSingleToFile(baseDirPath, "style.css", result.getCssCode());
        writeSingleToFile(baseDirPath, "script.js", result.getJsCode());
    }
}
