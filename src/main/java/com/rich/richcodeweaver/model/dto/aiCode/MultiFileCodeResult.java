package com.rich.richcodeweaver.model.dto.aiCode;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * AI 生成多代码文件的响应类
 *
 * @author DuRuiChi
 * @create 2025/8/5
 **/
@Description("生成多代码文件的结果")
@Data
public class MultiFileCodeResult {

    /**
     * HTML 代码块
     */
    @Description("HTML 代码块")
    private String htmlCode;

    /**
     * CSS 代码块
     */
    @Description("CSS 代码块")
    private String cssCode;

    /**
     * JS 代码块
     */
    @Description("JS 代码块")
    private String jsCode;

    /**
     * 代码描述
     */
    @Description("对生成的代码进行详细描述")
    private String description;
}