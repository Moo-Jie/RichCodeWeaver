package com.rich.richcodeweaver.model.dto.aiCode;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * AI 生成 HTML 代码响应类
 *
 * @author DuRuiChi
 * @create 2025/8/5
 **/
@Description("生成 HTML 代码文件的结果")
@Data
public class HtmlCodeResult {

    /**
     * HTML 代码块
     */
    @Description("HTML代码")
    private String htmlCode;

    /**
     * 代码描述
     */
    @Description("生成代码的描述")
    private String description;
}