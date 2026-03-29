package com.rich.ai.aiTools.planning;

import cn.hutool.json.JSONObject;
import com.rich.ai.aiTools.BaseTool;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 消息输出工具
 * 用于 Agent 向用户发送结构化的状态消息、进度更新、警告或错误信息
 * 
 * 使用场景：
 * - 通知用户当前执行阶段
 * - 报告进度百分比
 * - 发送警告或错误信息
 * - 请求用户确认或输入
 *
 * @author DuRuiChi
 * @create 2026/3/28
 **/
@Slf4j
@Component
public class MessageTool extends BaseTool {

    @Override
    public String getToolName() {
        return "sendMessage";
    }

    @Override
    public String getToolDisplayName() {
        return "发送消息";
    }

    @Override
    public String getResultMsg(JSONObject arguments, String result) {
        String type = arguments.getStr("type", "info");
        String title = arguments.getStr("title", "");
        
        // 构建包含消息标题的结果消息
        StringBuilder sb = new StringBuilder();
        sb.append("[消息已发送] 类型: ").append(type);
        
        // 如果有标题，附加标题内容
        if (title != null && !title.trim().isEmpty()) {
            sb.append(" | ").append(title);
        }
        
        return sb.toString();
    }

    /**
     * 发送结构化消息给用户
     * 
     * @param type 消息类型：info（信息）、progress（进度）、warning（警告）、error（错误）、success（成功）
     * @param title 消息标题
     * @param content 消息内容
     * @param progress 进度百分比（仅 progress 类型时有效，0-100）
     * @param appId 产物ID
     * @return 消息发送确认
     */
    @Tool("向用户发送结构化消息，用于报告进度、状态更新、警告或错误信息。")
    public String sendMessage(
            @P("消息类型：info（信息）、progress（进度）、warning（警告）、error（错误）、success（成功）")
            String type,
            @P("消息标题")
            String title,
            @P("消息详细内容")
            String content,
            @P("进度百分比（仅progress类型时有效，0-100）")
            Integer progress,
            @ToolMemoryId Long appId) {

        if (title == null || title.trim().isEmpty()) {
            title = "状态更新";
        }

        String typeIcon = switch (type != null ? type.toLowerCase() : "info") {
            case "progress" -> "⏳";
            case "warning" -> "⚠️";
            case "error" -> "❌";
            case "success" -> "✅";
            default -> "ℹ️";
        };

        StringBuilder message = new StringBuilder();
        message.append("\n").append("═".repeat(40)).append("\n");
        message.append(typeIcon).append(" ").append(title).append("\n");
        message.append("─".repeat(40)).append("\n");

        if (content != null && !content.trim().isEmpty()) {
            message.append(content).append("\n");
        }

        if ("progress".equalsIgnoreCase(type) && progress != null) {
            int percent = Math.min(Math.max(progress, 0), 100);
            int filled = percent / 5;  // 20格进度条
            int empty = 20 - filled;
            message.append("\n进度: [");
            message.append("█".repeat(filled));
            message.append("░".repeat(empty));
            message.append("] ").append(percent).append("%\n");
        }

        message.append("═".repeat(40)).append("\n");

        log.info("[Message] appId={}, type={}, title={}", appId, type, title);

        return message.toString();
    }
}
