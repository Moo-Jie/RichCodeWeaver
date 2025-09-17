package com.rich.richcodeweaver.aiTools;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 供 AI 调用工具的管理类
 *
 * @author DuRuiChi
 * @create 2025/9/6
 **/
@Component
@Slf4j
public class ToolsManager {
    /**
     * 工具名与工具的映射
     */
    private final Map<String, BaseTool> toolsMap = new HashMap<>();
    /**
     * 初始化工具包
     */
    @Resource
    private BaseTool[] baseTools;

    /**
     * 初始化工具包
     */
    @PostConstruct
    public void toolsInit() {
        for (BaseTool baseTool : baseTools) {
            toolsMap.put(baseTool.getToolName(), baseTool);
            log.info("已初始化工具：{}", baseTool.getToolDisplayName());
        }
    }

    /**
     * 获取工具
     *
     * @param toolName 工具名
     * @return 工具
     */
    public BaseTool getToolByName(String toolName) {
        return toolsMap.get(toolName);
    }

    /**
     * 获取所有工具
     *
     * @return 所有工具
     */
    public BaseTool[] getAllTools() {
        return baseTools;
    }
}
