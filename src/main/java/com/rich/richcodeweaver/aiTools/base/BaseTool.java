package com.rich.richcodeweaver.aiTools.base;

import cn.hutool.json.JSONObject;

/**
 * 供 AI 调用工具的基类，供 AI 调用工具的通用接口
 *
 * @author DuRuiChi
 * @create 2025/9/6
 **/
public abstract class BaseTool {
    /**
     * 获取工具执行开始时的提示信息
     *
     * @return 工具请求显示内容
     */
    public String getResponseMsg() {
        return String.format("\n\n[开始调用系统工具] %s\n\n", getToolDisplayName());
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
     * @return 格式化的工具执行结果
     */
    public abstract String getResultMsg(JSONObject arguments);
}
