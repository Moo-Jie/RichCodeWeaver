package com.rich.ai.aiTools.planning;

import cn.hutool.json.JSONObject;
import com.rich.ai.aiTools.BaseTool;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 思考工具
 * 用于 Agent 进行复杂推理、反思和决策，不执行任何实际操作
 * 
 * 使用场景：
 * - 分析用户需求，确定技术方案
 * - 遇到问题时进行反思和调整策略
 * - 在执行关键操作前进行确认
 * - 总结当前进度和下一步计划
 *
 * @author DuRuiChi
 * @create 2026/3/28
 **/
@Slf4j
@Component
public class ThinkTool extends BaseTool {

    @Override
    public String getToolName() {
        return "think";
    }

    @Override
    public String getToolDisplayName() {
        return "思考与推理";
    }

    @Override
    public String getResultMsg(JSONObject arguments) {
        String category = arguments.getStr("category", "general");
        return "[思考完成] 类型: " + category;
    }

    /**
     * 进行思考和推理
     * 
     * @param category 思考类型：analyze（需求分析）、plan（规划）、reflect（反思）、decide（决策）、summarize（总结）
     * @param thought 思考内容
     * @param conclusion 得出的结论或决定
     * @param appId 产物ID
     * @return 思考记录确认
     */
    @Tool("进行深度思考和推理。在分析需求、规划方案、遇到问题反思、做重要决策前使用此工具记录思考过程。")
    public String think(
            @P("思考类型：analyze（需求分析）、plan（规划）、reflect（反思）、decide（决策）、summarize（总结）")
            String category,
            @P("详细的思考内容，包括分析过程、考虑的因素、权衡的选项等")
            String thought,
            @P("得出的结论或决定")
            String conclusion,
            @ToolMemoryId Long appId) {

        if (thought == null || thought.trim().isEmpty()) {
            return "错误：思考内容不能为空";
        }

        String categoryLabel = switch (category != null ? category.toLowerCase() : "general") {
            case "analyze" -> "📊 需求分析";
            case "plan" -> "📋 方案规划";
            case "reflect" -> "🔍 问题反思";
            case "decide" -> "⚖️ 决策判断";
            case "summarize" -> "📝 进度总结";
            default -> "💭 一般思考";
        };

        log.info("[Think] appId={}, category={}, thought={}", appId, category, 
                thought.length() > 100 ? thought.substring(0, 100) + "..." : thought);

        StringBuilder response = new StringBuilder();
        response.append("─".repeat(40)).append("\n");
        response.append(categoryLabel).append("\n");
        response.append("─".repeat(40)).append("\n");
        response.append("【思考过程】\n").append(thought).append("\n\n");
        
        if (conclusion != null && !conclusion.trim().isEmpty()) {
            response.append("【结论】\n").append(conclusion).append("\n");
        }
        
        response.append("─".repeat(40)).append("\n");
        response.append("思考已记录，请继续执行下一步操作。");

        return response.toString();
    }
}
