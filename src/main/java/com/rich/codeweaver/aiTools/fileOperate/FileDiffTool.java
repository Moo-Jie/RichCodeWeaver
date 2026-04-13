package com.rich.codeweaver.aiTools.fileOperate;

import cn.hutool.json.JSONObject;
import com.rich.codeweaver.aiTools.BaseTool;
import com.rich.codeweaver.common.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件差异对比工具
 * 用于比较两个文件的差异，或查看文件修改前后的变化
 *
 * @author DuRuiChi
 * @create 2026/3/28
 **/
@Slf4j
@Component
public class FileDiffTool extends BaseTool {

    @Override
    public String getToolName() {
        return "diffFile";
    }

    @Override
    public String getToolDisplayName() {
        return "文件差异对比";
    }

    @Override
    public String getResultMsg(JSONObject arguments, String result) {
        return "[工具调用结束] 文件差异对比完成";
    }

    /**
     * 比较两段文本的差异
     *
     * @param originalContent 原始内容
     * @param newContent      新内容
     * @param contextLines    上下文行数（显示差异周围的行数，默认3）
     * @param appId           产物ID
     * @return 差异报告
     */
    @Tool("比较两段文本的差异，用于代码审查或修改确认。")
    public String diffContent(
            @P("原始内容")
            String originalContent,
            @P("新内容")
            String newContent,
            @P("上下文行数（显示差异周围的行数，默认3）")
            Integer contextLines,
            @ToolMemoryId Long appId) {

        if (originalContent == null) {
            originalContent = "";
        }
        if (newContent == null) {
            newContent = "";
        }

        int context = contextLines != null ? Math.max(0, Math.min(contextLines, 10)) : 3;

        String[] originalLines = originalContent.split("\n", -1);
        String[] newLines = newContent.split("\n", -1);

        StringBuilder diff = new StringBuilder();
        diff.append("─".repeat(50)).append("\n");
        diff.append("📊 文件差异对比\n");
        diff.append("─".repeat(50)).append("\n");
        diff.append(String.format("原始: %d 行 | 新版: %d 行\n", originalLines.length, newLines.length));
        diff.append("─".repeat(50)).append("\n\n");

        // 简单的逐行对比（LCS 算法的简化版本）
        int i = 0, j = 0;
        int additions = 0, deletions = 0, modifications = 0;

        while (i < originalLines.length || j < newLines.length) {
            if (i >= originalLines.length) {
                // 新增的行
                diff.append(String.format("+ %4d | %s\n", j + 1, newLines[j]));
                additions++;
                j++;
            } else if (j >= newLines.length) {
                // 删除的行
                diff.append(String.format("- %4d | %s\n", i + 1, originalLines[i]));
                deletions++;
                i++;
            } else if (originalLines[i].equals(newLines[j])) {
                // 相同的行（只在有差异的上下文中显示）
                i++;
                j++;
            } else {
                // 不同的行
                diff.append(String.format("- %4d | %s\n", i + 1, originalLines[i]));
                diff.append(String.format("+ %4d | %s\n", j + 1, newLines[j]));
                modifications++;
                i++;
                j++;
            }
        }

        diff.append("\n").append("─".repeat(50)).append("\n");
        diff.append(String.format("统计: +%d 新增 | -%d 删除 | ~%d 修改\n", additions, deletions, modifications));
        diff.append("─".repeat(50));

        log.info("[FileDiff] appId={}, additions={}, deletions={}, modifications={}", 
                appId, additions, deletions, modifications);

        return diff.toString();
    }

    /**
     * 比较项目中两个文件的差异
     *
     * @param filePath1 第一个文件的相对路径
     * @param filePath2 第二个文件的相对路径
     * @param appId     产物ID
     * @return 差异报告
     */
    @Tool("比较项目中两个文件的差异")
    public String diffFiles(
            @P("第一个文件的相对路径")
            String filePath1,
            @P("第二个文件的相对路径")
            String filePath2,
            @ToolMemoryId Long appId) {

        if (filePath1 == null || filePath1.trim().isEmpty()) {
            return "错误：第一个文件路径不能为空";
        }
        if (filePath2 == null || filePath2.trim().isEmpty()) {
            return "错误：第二个文件路径不能为空";
        }

        try {
            String codeGenType = CreatAndWriteAiTool.APP_CODE_GEN_TYPE_CACHE.getOrDefault(appId, "vue_project");
            String projectDirName = codeGenType + "_" + appId;
            Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);

            Path path1 = projectRoot.resolve(filePath1);
            Path path2 = projectRoot.resolve(filePath2);

            if (!Files.exists(path1)) {
                return "错误：文件不存在 - " + filePath1;
            }
            if (!Files.exists(path2)) {
                return "错误：文件不存在 - " + filePath2;
            }

            String content1 = Files.readString(path1);
            String content2 = Files.readString(path2);

            return diffContent(content1, content2, 3, appId);

        } catch (IOException e) {
            log.error("[FileDiff] 读取文件失败", e);
            return "错误：读取文件失败 - " + e.getMessage();
        }
    }
}
