package com.rich.richcodeweaver.utiles.aiUtils.codeParse;

import com.rich.richcodeweaver.model.aiChatResponse.codeResponse.MultiFileCodeResponse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Html 文件解析策略的具体逻辑实现
 * (基于策略模式，不同的代码类型有不同的解析模式)
 *
 * @author DuRuiChi
 * @create 2025/8/7
 **/
public class AiResToMultiFileCodeResultParser implements CodeParser<MultiFileCodeResponse> {
    /**
     * 匹配 HTML 代码块的正则模式（不区分大小写）
     */
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile(
            "```html\\s*\n([\\s\\S]*?)```",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 匹配 CSS 代码块的正则模式（不区分大小写）
     */
    private static final Pattern CSS_CODE_PATTERN = Pattern.compile(
            "```css\\s*\n([\\s\\S]*?)```",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 匹配 JavaScript 代码块的正则模式（不区分大小写）
     */
    private static final Pattern JS_CODE_PATTERN = Pattern.compile(
            "```(?:js|javascript)\\s*\n([\\s\\S]*?)```",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 解析多文件模式下的代码内容
     *
     * @param codeContent 包含多种代码类型的原始文本
     * @return MultiFileCodeResponse 包含解析后的各类型代码
     * <p>
     * 支持解析的类型：
     * - HTML代码块（```html）
     * - CSS代码块（```css）
     * - JavaScript代码块（```js 或 ```javascript）
     */
    public MultiFileCodeResponse parseCode(String codeContent) {
        MultiFileCodeResponse result = new MultiFileCodeResponse();

        // 分别提取各类型代码
        String htmlCode = extractCodeByPattern(codeContent, HTML_CODE_PATTERN);
        String cssCode = extractCodeByPattern(codeContent, CSS_CODE_PATTERN);
        String jsCode = extractCodeByPattern(codeContent, JS_CODE_PATTERN);

        // 设置HTML代码（非空时处理）
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
        }

        // 设置CSS代码（非空时处理）
        if (cssCode != null && !cssCode.trim().isEmpty()) {
            result.setCssCode(cssCode.trim());
        }

        // 设置JavaScript代码（非空时处理）
        if (jsCode != null && !jsCode.trim().isEmpty()) {
            result.setJsCode(jsCode.trim());
        }

        return result;
    }

    /**
     * 通用代码提取方法
     *
     * @param content 原始文本内容
     * @param pattern 匹配用的正则模式
     * @return 提取到的代码内容，未找到时返回null
     * <p>
     * 该方法使用预定义的正则表达式模式进行代码块匹配，
     * 适用于各种类型的代码块提取。
     */
    private static String extractCodeByPattern(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }
}
