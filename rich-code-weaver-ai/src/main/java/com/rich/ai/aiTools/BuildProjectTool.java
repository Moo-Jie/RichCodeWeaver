package com.rich.ai.aiTools;

import cn.hutool.json.JSONObject;
import com.rich.common.constant.AppConstant;
import com.rich.common.utils.deployWebProjectUtils.BuildWebProjectExecutor;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Paths;

/**
 * Agent 工作流工具：同步构建 Vue 工程项目
 * 仅在 vue_project 类型时调用。构建完成后将结果（成功或完整错误日志）返回给 AI，
 * 让 AI 根据错误信息修复代码并重新构建。
 *
 * @author DuRuiChi
 * @create 2026/4/1
 **/
@Slf4j
@Component
public class BuildProjectTool extends BaseTool {

    @Resource
    private BuildWebProjectExecutor buildWebProjectExecutor;

    @Override
    public String getToolName() {
        return "buildProject";
    }

    @Override
    public String getToolDisplayName() {
        return "构建 Vue 工程项目";
    }

    @Override
    public String getResultMsg(JSONObject arguments, String result) {
        return "[工具调用结束] Vue 项目构建流程已执行，请查看返回结果";
    }

    /**
     * 同步构建 Vue 工程项目，返回详细结果供 AI 修复错误
     * 构建过程：npm install → npm run build → 验证 dist 目录
     *
     * @param appId 产物 ID（自动注入）
     * @return 构建成功消息或详细错误日志
     */
    @Tool("【vue_project 类型必须调用】所有项目文件创建完毕后，调用此工具构建 Vue 工程项目。" +
            "构建失败时会返回详细错误日志，请根据错误修复代码后再次调用。")
    public String buildProject(@ToolMemoryId Long appId) {

        String projectPath = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, "vue_project_" + appId)
                .toAbsolutePath()
                .toString();

        log.info("Agent 触发项目构建: appId={}, projectPath={}", appId, projectPath);

        // 验证项目目录是否存在
        File projectDir = new File(projectPath);
        if (!projectDir.exists()) {
            return "构建失败：项目目录不存在 " + projectPath
                    + "\n请先使用 creatAndWrite 工具创建项目文件（至少需要 package.json）再调用此工具。";
        }

        // 执行同步构建，捕获完整日志
        BuildWebProjectExecutor.BuildResult result = buildWebProjectExecutor.buildProjectWithLog(projectPath);

        if (result.success()) {
            log.info("项目构建成功: appId={}", appId);
            return "构建成功！\n" + result.log()
                    + "\n\n【后续操作】构建完成，现在可以调用 exit 工具结束任务。";
        } else {
            log.warn("项目构建失败: appId={}, phase={}", appId, result.phase());
            return "构建失败（阶段: " + result.phase() + "）\n\n"
                    + "【错误日志】\n" + result.log()
                    + "\n\n【修复建议】\n"
                    + "1. 仔细阅读上方错误日志，定位问题文件和行号\n"
                    + "2. 使用 modifyFile 工具修复对应文件中的错误\n"
                    + "3. 修复后再次调用 buildProject 重新构建\n"
                    + "4. 如果连续失败 3 次以上，请简化实现方案后重试";
        }
    }
}
