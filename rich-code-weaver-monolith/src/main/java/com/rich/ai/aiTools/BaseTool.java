package com.rich.ai.aiTools;

import cn.hutool.json.JSONObject;

/**
 * 供 AI 调用工具的基类，供 AI 调用工具的通用接口
 *
 * @author DuRuiChi
 * @create 2026/1/6
 **/
public abstract class BaseTool {
    /**
     * 获取工具执行开始时的提示信息
     * 用于在AI调用工具时向用户显示工具调用的开始提示
     *
     * @return 工具请求显示内容，格式化后的提示信息
     */
    public String getResponseMsg() {
        // 获取工具的显示名称
        String displayName = getToolDisplayName();

        // 防御性编程：处理工具名称为null或空的情况
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = "未知工具";
        }

        // 返回格式化的工具调用开始提示信息
        return String.format("\n\n[开始调用系统工具] %s\n\n", displayName);
    }

    /**
     * 获取工具的英文名称
     *
     * @return 工具英文名称
     */
    public abstract String getToolName();

    /**
     * 获取工具的名称
     *
     * @return 工具中文名称
     */
    public abstract String getToolDisplayName();

    /**
     * 获取工具执行结束后的结果
     *
     * @param arguments 工具执行参数
     * @param result    工具执行的实际结果（可选，部分工具需要从结果中提取信息）
     * @return 格式化的工具执行结果
     */
    public abstract String getResultMsg(JSONObject arguments, String result);
}
