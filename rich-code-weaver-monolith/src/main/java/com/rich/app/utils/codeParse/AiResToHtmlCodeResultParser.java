package com.rich.app.utils.codeParse;

import com.rich.ai.model.codeResponse.HtmlCodeResponse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Html 文件解析策略的具体逻辑实现
 * (基于策略模式，不同的代码类型有不同的解析模式)
 *
 * @author DuRuiChi
 * @create 2025/12/7
 **/
public class AiResToHtmlCodeResultParser implements CodeParser<HtmlCodeResponse> {
    /**
     * 匹配HTML代码块的正则模式（不区分大小写）
     */
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile(
            "```html\\s*\n([\\s\\S]*?)```",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 提取HTML代码内容
     *
     * @param content 原始文本内容
     * @return 提取到的HTML代码，未找到时返回null
     */
    private static String extractHtmlCode(String content) {
        // 使用正则表达式匹配 ```html ... ``` 代码块
        Matcher matcher = HTML_CODE_PATTERN.matcher(content);
        // 如果找到匹配项，返回代码块内容（group(1)）；否则返回 null
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * 解析单文件模式下的HTML内容
     *
     * @param codeContent 包含HTML代码的原始文本
     * @return HtmlCodeResponse 包含解析后的HTML代码
     * 处理逻辑：
     * 1. 尝试从内容中提取标准HTML代码块
     * 2. 如果未找到代码块，则将整个输入内容视为HTML代码
     * 3. 自动去除代码前后的空白字符
     */
    public HtmlCodeResponse parseCode(String codeContent) {
        // 创建 HTML 代码响应对象
        HtmlCodeResponse result = new HtmlCodeResponse();

        // 步骤1：尝试从内容中提取标准 HTML 代码块（```html ... ```）
        String htmlCode = extractHtmlCode(codeContent);

        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            // 成功提取代码块的情况：使用提取到的 HTML 代码
            result.setHtmlCode(htmlCode.trim());
        } else {
            // 未找到代码块的备用处理方案：将整个输入内容视为 HTML 代码
            // 这种情况适用于 AI 直接返回 HTML 代码而不使用代码块标记的场景
            result.setHtmlCode(codeContent.trim());
        }
        return result;
    }
}
