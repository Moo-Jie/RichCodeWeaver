package com.rich.richcodeweaver.utiles.codeSave;

import com.rich.richcodeweaver.model.dto.ai.HtmlCodeResult;
import com.rich.richcodeweaver.model.enums.CodeGeneratorTypeEnum;

/**
 * HTML代码文件保存器
 *
 * @author yupi
 */
public class HtmlCodeSaver extends CodeResultSaveToFileTemplate<HtmlCodeResult> {

    @Override
    protected String getCodeGeneratorTypeEnumValue() {
        return CodeGeneratorTypeEnum.HTML.getValue();
    }

    @Override
    protected void doSaveCodeResult(HtmlCodeResult result, String baseDirPath) {
        // 重写保存逻辑，直接调用单文件写入模板方法
        writeSingleToFile(baseDirPath, "index.html", result.getHtmlCode());
    }
}
