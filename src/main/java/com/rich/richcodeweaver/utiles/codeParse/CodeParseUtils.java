package com.rich.richcodeweaver.utiles.codeParse;

import com.rich.richcodeweaver.model.dto.aiCode.HtmlCodeResult;
import com.rich.richcodeweaver.model.dto.aiCode.MultiFileCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 生成代码解析工具类，用于从 AI 生成的响应内容中提取结构化代码片段（已弃用，拆解为分策略模式 + 执行器模式）
 *
 * @author DuRuiChi
 * @create 2025/8/6
 * @deprecated 建议使用新的代码解析模块替代
 **/
@Deprecated
public class CodeParseUtils {

    // 匹配HTML代码块的正则模式（不区分大小写）
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile(
            "```html\\s*\n([\\s\\S]*?)```",
            Pattern.CASE_INSENSITIVE
    );

    // 匹配CSS代码块的正则模式（不区分大小写）
    private static final Pattern CSS_CODE_PATTERN = Pattern.compile(
            "```css\\s*\n([\\s\\S]*?)```",
            Pattern.CASE_INSENSITIVE
    );

    // 匹配JavaScript代码块的正则模式（不区分大小写）
    private static final Pattern JS_CODE_PATTERN = Pattern.compile(
            "```(?:js|javascript)\\s*\n([\\s\\S]*?)```",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 解析单文件模式下的HTML内容
     *
     * @param codeContent 包含HTML代码的原始文本
     * @return HtmlCodeResult 包含解析后的HTML代码
     * <p>
     * 处理逻辑：
     * 1. 尝试从内容中提取标准HTML代码块
     * 2. 如果未找到代码块，则将整个输入内容视为HTML代码
     * 3. 自动去除代码前后的空白字符
     */
    public static HtmlCodeResult parseHtmlCode(String codeContent) {
        HtmlCodeResult result = new HtmlCodeResult();

        // 提取HTML代码
        String htmlCode = extractHtmlCode(codeContent);

        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            // 成功提取代码块的情况
            result.setHtmlCode(htmlCode.trim());
        } else {
            // 未找到代码块的备用处理方案
            result.setHtmlCode(codeContent.trim());
        }
        return result;
    }

    /**
     * 解析多文件模式下的代码内容
     *
     * @param codeContent 包含多种代码类型的原始文本
     * @return MultiFileCodeResult 包含解析后的各类型代码
     * <p>
     * 支持解析的类型：
     * - HTML代码块（```html）
     * - CSS代码块（```css）
     * - JavaScript代码块（```js 或 ```javascript）
     */
    public static MultiFileCodeResult parseMultiFileCode(String codeContent) {
        MultiFileCodeResult result = new MultiFileCodeResult();

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

    /**
     * 提取HTML代码内容
     *
     * @param content 原始文本内容
     * @return 提取到的HTML代码，未找到时返回null
     */
    private static String extractHtmlCode(String content) {
        Matcher matcher = HTML_CODE_PATTERN.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }
}