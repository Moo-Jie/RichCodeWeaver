package com.rich.app.utils.streamHandle;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationOutputFormatterTest {

    @Test
    void shouldRenderNeutralMarkdownSection() {
        String markdown = ConversationOutputFormatter.section(
                "资源准备",
                "已完成",
                List.of(
                        ConversationOutputFormatter.item("网络资料", "1,240 字符"),
                        ConversationOutputFormatter.item("图片素材", "3 项")
                ));

        assertTrue(markdown.contains("## 资源准备"));
        assertTrue(markdown.contains("状态：已完成"));
        assertTrue(markdown.contains("- 网络资料：1,240 字符"));
        assertFalse(markdown.contains("<div"));
        assertFalse(markdown.matches("(?s).*[\\p{So}\\p{Sk}].*"));
    }

    @Test
    void shouldEncodeTaskPlanWithoutExposingInternalPayload() {
        String taskPlanJson = "{\"type\":\"taskPlan\",\"tasks\":[{\"id\":1,\"step\":\"创建页面\",\"status\":\"completed\"}]}";
        String markdown = ConversationOutputFormatter.taskPlanMarker(taskPlanJson);

        assertTrue(markdown.contains("<!--CW_TASK_PLAN:"));
        assertTrue(ConversationOutputFormatter.isTransientMarker(markdown));
        assertFalse(markdown.contains("创建页面"));

        int start = markdown.indexOf("<!--CW_TASK_PLAN:") + "<!--CW_TASK_PLAN:".length();
        int end = markdown.indexOf("-->", start);
        String decoded = new String(
                Base64.getDecoder().decode(markdown.substring(start, end)),
                StandardCharsets.UTF_8);
        assertEquals(taskPlanJson, decoded);
    }

    @Test
    void shouldKeepFinalStatusOutsideCollapsedSections() {
        String markdown = ConversationOutputFormatter.completion("可查看预览、部署或下载");

        assertTrue(markdown.contains("<!--CW_FINAL_STATUS-->"));
        assertTrue(markdown.contains("> 生成完成 ·"));
    }
}