package com.rich.app.utils.streamHandle;

import cn.hutool.core.util.StrUtil;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * 对话输出协议：统一 Agent 与工作流模式的可展示 Markdown。
 *
 * <p>该协议只生成标准 Markdown，不使用 emoji、装饰图标或后端样式类。前端仅负责
 * Markdown 排版、标题折叠和少量固定语义（状态行）呈现。</p>
 *
 * @author DuRuiChi
 * @create 2026/11/06
 */
public final class ConversationOutputFormatter {

    /**
     * 任务计划隐藏标记前缀。负载为 UTF-8 JSON 的 Base64，避免任务文本破坏 HTML 注释。
     */
    private static final String TASK_PLAN_MARKER_PREFIX = "<!--CW_TASK_PLAN:";

    /**
     * 任务计划隐藏标记后缀。
     */
    private static final String TASK_PLAN_MARKER_SUFFIX = "-->";

    /**
     * 最终状态边界标记，确保完成/失败提示不会被最后一个折叠阶段吞掉。
     */
    private static final String FINAL_STATUS_MARKER = "<!--CW_FINAL_STATUS-->";

    private ConversationOutputFormatter() {
    }

    /**
     * 构建对话文档标题。
     *
     * @param title       文档标题
     * @param contextLine 模式与类型说明
     * @return Markdown 文档头
     */
    public static String documentHeader(String title, String contextLine) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("\n\n# ").append(safeInline(title)).append("\n\n");
        if (StrUtil.isNotBlank(contextLine)) {
            markdown.append("> ").append(safeInline(contextLine)).append("\n\n");
        }
        return markdown.toString();
    }

    /**
     * 构建可折叠的阶段区块。
     *
     * @param title   阶段标题
     * @param status  阶段状态
     * @param details 阶段摘要
     * @return Markdown 阶段区块
     */
    public static String section(String title, String status, List<String> details) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("\n\n## ").append(safeInline(title)).append("\n\n");
        markdown.append(statusLine(status));
        markdown.append(bulletList(details));
        return markdown.toString();
    }

    /**
     * 为已经流式开始的阶段追加完成内容，不重复输出标题。
     *
     * @param status  阶段状态
     * @param details 阶段摘要
     * @return Markdown 阶段正文
     */
    public static String sectionBody(String status, List<String> details) {
        return "\n\n" + statusLine(status) + bulletList(details);
    }

    /**
     * 阶段刚开始时输出标题与进行中状态。
     *
     * @param title       阶段标题
     * @param description 当前动作
     * @return Markdown 阶段开头
     */
    public static String phaseStarted(String title, String description) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("\n\n## ").append(safeInline(title)).append("\n\n");
        if (StrUtil.isNotBlank(description)) {
            markdown.append(safeInline(description)).append("\n\n");
        }
        return markdown.toString();
    }

    /**
     * 构建工具执行完成的一行摘要。
     *
     * @param displayName 工具名称
     * @param detail      工具执行摘要
     * @return Markdown 引用行
     */
    public static String toolCompleted(String displayName, String detail) {
        StringBuilder line = new StringBuilder("已执行 · ").append(safeInline(displayName));
        if (StrUtil.isNotBlank(detail)) {
            line.append(" · ").append(safeInline(detail));
        }
        return "\n\n> " + line + "\n\n";
    }

    /**
     * 构建成功完成提示。
     *
     * @param message 结果说明
     * @return Markdown 引用行
     */
    public static String completion(String message) {
        return "\n\n" + FINAL_STATUS_MARKER + "\n\n> 生成完成 · " + safeInline(message) + "\n\n";
    }

    /**
     * 构建失败提示。
     *
     * @param message 失败说明
     * @return Markdown 引用行
     */
    public static String failure(String message) {
        return "\n\n" + FINAL_STATUS_MARKER + "\n\n> 生成未完成 · " + safeInline(message) + "\n\n";
    }

    /**
     * 构建统一状态行。
     *
     * @param status 状态文本
     * @return Markdown 状态段落
     */
    public static String statusLine(String status) {
        String normalized = StrUtil.blankToDefault(status, "进行中");
        return "状态：" + safeInline(normalized) + "\n\n";
    }

    /**
     * 构建摘要条目。
     *
     * @param label 条目标签
     * @param value 条目值
     * @return Markdown 列表项
     */
    public static String item(String label, String value) {
        return safeInline(label) + "：" + safeInline(value);
    }

    /**
     * 将任务计划 JSON 编码为前端可识别的隐藏标记。
     *
     * @param taskPlanJson 任务计划 JSON
     * @return 隐藏 Markdown 注释标记
     */
    public static String taskPlanMarker(String taskPlanJson) {
        if (StrUtil.isBlank(taskPlanJson)) {
            return "";
        }
        String encoded = Base64.getEncoder().encodeToString(taskPlanJson.getBytes(StandardCharsets.UTF_8));
        return "\n\n" + TASK_PLAN_MARKER_PREFIX + encoded + TASK_PLAN_MARKER_SUFFIX + "\n\n";
    }

    /**
     * 判断是否为只供实时 UI 使用、不写入对话历史的临时标记。
     *
     * @param markdown 协议内容
     * @return 是否应跳过历史落库
     */
    public static boolean isTransientMarker(String markdown) {
        return markdown != null && markdown.contains(TASK_PLAN_MARKER_PREFIX);
    }

    /**
     * 单行安全文本：移除换行并限制长度，避免摘要破坏 Markdown 结构。
     *
     * @param value 原始文本
     * @return 单行文本
     */
    public static String safeInline(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace("\r", " ").replace("\n", " ").replaceAll("\\s+", " ").trim();
        return clip(normalized, 240)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("*", "\\*")
                .replace("_", "\\_")
                .replace("[", "\\[")
                .replace("]", "\\]");
    }

    /**
     * 截断文本并追加省略号。
     *
     * @param value     原始文本
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    public static String clip(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    /**
     * 格式化字符数量，便于阶段摘要保持一致。
     *
     * @param count 字符数量
     * @return 带千分位的数量
     */
    public static String count(long count) {
        return String.format(Locale.ROOT, "%,d", Math.max(0, count));
    }

    private static String bulletList(List<String> details) {
        if (details == null || details.isEmpty()) {
            return "";
        }
        StringBuilder markdown = new StringBuilder();
        for (String detail : details) {
            if (StrUtil.isNotBlank(detail)) {
                markdown.append("- ").append(safeInline(detail)).append("\n");
            }
        }
        return markdown.length() == 0 ? "" : markdown.append("\n").toString();
    }
}
