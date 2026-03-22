package com.rich.ai.aiTools;

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
 * @create 2026/1/6
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
     * 在Spring容器初始化完成后自动执行，将所有工具注册到工具映射表中
     */
    @PostConstruct
    public void toolsInit() {
        // 参数校验：检查工具数组是否为空
        if (baseTools == null || baseTools.length == 0) {
            log.warn("未检测到任何可用工具，工具管理器将为空");
            return;
        }
        
        // 遍历所有工具并注册到映射表中
        for (BaseTool tool : baseTools) {
            // 跳过null工具（防御性编程）
            if (tool == null) {
                log.warn("检测到null工具，已跳过");
                continue;
            }
            
            // 获取工具名称并校验
            String toolName = tool.getToolName();
            if (toolName == null || toolName.trim().isEmpty()) {
                log.warn("检测到工具名称为空的工具，已跳过: {}", tool.getClass().getSimpleName());
                continue;
            }
            
            // 检查是否存在重复的工具名称
            if (toolsMap.containsKey(toolName)) {
                log.warn("检测到重复的工具名称: {}，旧工具将被覆盖", toolName);
            }
            
            // 将工具注册到映射表中
            toolsMap.put(toolName, tool);
            log.info("已初始化工具：{} ({})", tool.getToolDisplayName(), toolName);
        }
        
        log.info("工具初始化完成，共注册 {} 个工具", toolsMap.size());
    }

    /**
     * 根据工具名称获取工具实例
     *
     * @param toolName 工具名称（英文标识符）
     * @return 工具实例，如果未找到则返回null
     */
    public BaseTool getToolByName(String toolName) {
        // 参数校验：检查工具名称是否为空
        if (toolName == null || toolName.trim().isEmpty()) {
            log.warn("尝试获取工具时传入的工具名称为空");
            return null;
        }
        
        // 从映射表中获取工具
        BaseTool tool = toolsMap.get(toolName);
        
        // 记录未找到工具的情况
        if (tool == null) {
            log.debug("未找到名称为 '{}' 的工具", toolName);
        }
        
        return tool;
    }

    /**
     * 获取所有已注册的工具数组
     *
     * @return 所有工具的数组，如果没有工具则返回空数组（不会返回null）
     */
    public BaseTool[] getAllTools() {
        // 防御性编程：确保不返回null
        if (baseTools == null) {
            log.warn("工具数组为null，返回空数组");
            return new BaseTool[0];
        }
        
        return baseTools;
    }
}
