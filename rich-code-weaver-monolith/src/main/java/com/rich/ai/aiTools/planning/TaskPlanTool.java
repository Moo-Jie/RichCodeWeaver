package com.rich.ai.aiTools.planning;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rich.ai.aiTools.BaseTool;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务计划工具
 * 用于 Agent 管理任务列表，跟踪任务进度，支持创建、更新、完成任务
 * 
 * 功能：
 * - 创建任务计划（分解复杂需求为多个子任务）
 * - 更新任务状态（pending → in_progress → completed）
 * - 查看当前任务列表
 * - 标记任务完成
 * 
 * 返回格式：
 * - 返回结构化 JSON，前端可直接解析并渲染任务列表面板
 * - 格式：{"action": "xxx", "tasks": [...], "summary": {...}, "message": "xxx"}
 *
 * @author DuRuiChi
 * @create 2026/3/28
 **/
@Slf4j
@Component
public class TaskPlanTool extends BaseTool {

    /**
     * 任务计划缓存，按 appId 隔离
     * key: appId, value: JSONArray of tasks
     * 每个 task: {id, step, status, notes}
     */
    public static final ConcurrentHashMap<Long, JSONArray> TASK_PLANS = new ConcurrentHashMap<>();

    @Override
    public String getToolName() {
        return "taskPlan";
    }

    @Override
    public String getToolDisplayName() {
        return "任务计划管理";
    }

    @Override
    public String getResultMsg(JSONObject arguments, String result) {
        // 直接返回工具执行的实际结果，包含完整的任务列表信息
        // result 格式示例: "任务计划已创建\n─────...\n[任务计划] {JSON}"
        if (result != null && result.contains("[任务计划]")) {
            // 提取 [任务计划] 后的 JSON 部分
            int idx = result.lastIndexOf("[任务计划]");
            return result.substring(idx);
        }
        // 兜底：返回简单格式
        String action = arguments.getStr("action");
        return "[任务计划] {\"type\":\"taskPlan\",\"action\":\"" + action + "\"}";
    }
    
    /**
     * 构建任务计划结果消息（供前端解析渲染）
     */
    private String buildTaskPlanResultMsg(String action, Long appId) {
        JSONObject result = new JSONObject();
        result.set("type", "taskPlan");
        result.set("action", action);
        
        // appId 为 null 时直接返回空任务列表
        if (appId == null) {
            return "[任务计划] " + JSONUtil.toJsonStr(result);
        }
        
        JSONArray tasks = TASK_PLANS.get(appId);
        
        if (tasks != null && !tasks.isEmpty()) {
            result.set("tasks", tasks);
            
            // 统计信息
            int completed = 0, inProgress = 0, pending = 0;
            for (int i = 0; i < tasks.size(); i++) {
                String status = tasks.getJSONObject(i).getStr("status", "pending");
                switch (status) {
                    case "completed" -> completed++;
                    case "in_progress" -> inProgress++;
                    default -> pending++;
                }
            }
            JSONObject summary = new JSONObject();
            summary.set("total", tasks.size());
            summary.set("completed", completed);
            summary.set("inProgress", inProgress);
            summary.set("pending", pending);
            result.set("summary", summary);
        }
        
        return "[任务计划] " + JSONUtil.toJsonStr(result);
    }

    /**
     * 管理任务计划
     * 
     * @param action 操作类型：create（创建计划）、update（更新任务状态）、list（查看任务列表）、complete（标记完成）
     * @param tasksJson 任务列表JSON（仅 create 时需要），格式：[{"step": "任务描述", "status": "pending"}]
     * @param taskId 任务ID（仅 update/complete 时需要）
     * @param newStatus 新状态（仅 update 时需要）：pending、in_progress、completed
     * @param notes 备注信息（可选）
     * @param appId 产物ID
     * @return 操作结果
     */
    @Tool("管理任务计划：创建任务列表、更新任务状态、查看进度。在开始复杂任务前先创建计划，执行过程中更新状态。")
    public String taskPlan(
            @P("操作类型：create（创建计划）、update（更新状态）、list（查看列表）、complete（标记完成）")
            String action,
            @P("任务列表JSON，仅create时需要，格式：[{\"step\": \"任务描述\", \"status\": \"pending\"}]")
            String tasksJson,
            @P("任务ID（从0开始），仅update/complete时需要")
            Integer taskId,
            @P("新状态，仅update时需要：pending、in_progress、completed")
            String newStatus,
            @P("备注信息（可选）")
            String notes,
            @ToolMemoryId Long appId) {

        if (action == null || action.trim().isEmpty()) {
            return "错误：操作类型不能为空";
        }

        return switch (action.toLowerCase()) {
            case "create" -> createPlan(tasksJson, appId);
            case "update" -> updateTask(taskId, newStatus, notes, appId);
            case "list" -> listTasks(appId);
            case "complete" -> completeTask(taskId, notes, appId);
            default -> "错误：不支持的操作类型 '" + action + "'，请使用: create、update、list、complete";
        };
    }

    /**
     * 创建任务计划
     */
    private String createPlan(String tasksJson, Long appId) {
        if (appId == null) {
            return "错误：appId 不能为空";
        }
        if (tasksJson == null || tasksJson.trim().isEmpty()) {
            return "错误：任务列表不能为空";
        }

        try {
            JSONArray tasks = JSONUtil.parseArray(tasksJson);
            
            // 为每个任务添加 ID
            for (int i = 0; i < tasks.size(); i++) {
                JSONObject task = tasks.getJSONObject(i);
                task.set("id", i);
                if (!task.containsKey("status")) {
                    task.set("status", "pending");
                }
            }
            
            TASK_PLANS.put(appId, tasks);
            log.info("创建任务计划，appId={}, 任务数={}", appId, tasks.size());
            
            return formatTaskList(tasks, "任务计划已创建");
        } catch (Exception e) {
            log.error("解析任务列表失败", e);
            return "错误：任务列表JSON格式不正确 - " + e.getMessage();
        }
    }

    /**
     * 更新任务状态
     */
    private String updateTask(Integer taskId, String newStatus, String notes, Long appId) {
        if (appId == null) {
            return "错误：appId 不能为空";
        }
        JSONArray tasks = TASK_PLANS.get(appId);
        if (tasks == null || tasks.isEmpty()) {
            return "错误：当前没有任务计划，请先使用 create 创建计划";
        }

        if (taskId == null || taskId < 0 || taskId >= tasks.size()) {
            return "错误：无效的任务ID，有效范围: 0-" + (tasks.size() - 1);
        }

        if (newStatus == null || newStatus.trim().isEmpty()) {
            return "错误：新状态不能为空";
        }

        if (!newStatus.matches("pending|in_progress|completed")) {
            return "错误：无效的状态值，请使用: pending、in_progress、completed";
        }

        JSONObject task = tasks.getJSONObject(taskId);
        String oldStatus = task.getStr("status");
        task.set("status", newStatus);
        if (notes != null && !notes.trim().isEmpty()) {
            task.set("notes", notes);
        }

        log.info("更新任务状态，appId={}, taskId={}, {} → {}", appId, taskId, oldStatus, newStatus);
        
        return formatTaskList(tasks, "任务 #" + taskId + " 状态已更新为 " + newStatus);
    }

    /**
     * 查看任务列表
     */
    private String listTasks(Long appId) {
        if (appId == null) {
            return "错误：appId 不能为空";
        }
        JSONArray tasks = TASK_PLANS.get(appId);
        if (tasks == null || tasks.isEmpty()) {
            return "当前没有任务计划。使用 create 操作创建新计划。";
        }
        
        return formatTaskList(tasks, "当前任务计划");
    }

    /**
     * 标记任务完成
     */
    private String completeTask(Integer taskId, String notes, Long appId) {
        return updateTask(taskId, "completed", notes, appId);
    }

    /**
     * 格式化任务列表输出
     * 返回格式包含两部分：
     * 1. 人类可读的任务列表文本
     * 2. [任务计划] JSON 标记（供前端解析渲染）
     */
    private String formatTaskList(JSONArray tasks, String header) {
        StringBuilder sb = new StringBuilder();
        sb.append(header).append("\n");
        sb.append("─".repeat(40)).append("\n");
        
        int completed = 0;
        int inProgress = 0;
        int pending = 0;
        
        for (int i = 0; i < tasks.size(); i++) {
            JSONObject task = tasks.getJSONObject(i);
            String status = task.getStr("status", "pending");
            String step = task.getStr("step", "未命名任务");
            String taskNotes = task.getStr("notes", "");
            
            String statusIcon = switch (status) {
                case "completed" -> "✅";
                case "in_progress" -> "🔄";
                default -> "⏳";
            };
            
            sb.append(String.format("%s #%d: %s", statusIcon, i, step));
            if (!taskNotes.isEmpty()) {
                sb.append(" (").append(taskNotes).append(")");
            }
            sb.append("\n");
            
            switch (status) {
                case "completed" -> completed++;
                case "in_progress" -> inProgress++;
                default -> pending++;
            }
        }
        
        sb.append("─".repeat(40)).append("\n");
        sb.append(String.format("进度: %d/%d 完成 | %d 进行中 | %d 待处理", 
                completed, tasks.size(), inProgress, pending));
        
        // 附加结构化 JSON 数据，供前端解析渲染任务面板
        JSONObject result = new JSONObject();
        result.set("type", "taskPlan");
        result.set("action", header.contains("创建") ? "create" : header.contains("更新") ? "update" : "list");
        result.set("tasks", tasks);
        JSONObject summary = new JSONObject();
        summary.set("total", tasks.size());
        summary.set("completed", completed);
        summary.set("inProgress", inProgress);
        summary.set("pending", pending);
        result.set("summary", summary);
        
        sb.append("\n[任务计划] ").append(JSONUtil.toJsonStr(result));
        
        return sb.toString();
    }

    /**
     * 清理指定 appId 的任务计划（用于任务完成后清理）
     */
    public static void clearPlan(Long appId) {
        TASK_PLANS.remove(appId);
    }
}
