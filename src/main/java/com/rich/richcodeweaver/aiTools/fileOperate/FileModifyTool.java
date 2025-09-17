package com.rich.richcodeweaver.aiTools.fileOperate;

import cn.hutool.json.JSONObject;
import com.rich.richcodeweaver.aiTools.BaseTool;
import com.rich.richcodeweaver.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 文件修改工具,支持 AI 通过工具调用的方式修改文件内容
 *
 * @author DuRuiChi
 * @create 2025/9/6
 **/
@Slf4j
@Component
public class FileModifyTool extends BaseTool {

    @Override
    public String getToolName() {
        return "modifyFile";
    }

    @Override
    public String getToolDisplayName() {
        return "文件修改工具";
    }

    @Override
    public String getResultMsg(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        String oldContent = arguments.getStr("oldContent");
        String newContent = arguments.getStr("newContent");
        relativeFilePath = relativeFilePath == null || relativeFilePath.isEmpty() ? "" : "\n[\n" + relativeFilePath + "\n]\n";
        // 显示对比内容
        return String.format("""
                [工具调用结束] %s %s
                
                替换前内容：
                ```
                %s
                ```
                
                替换后内容：
                ```
                %s
                ```
                """, "成功替换以下文件目录", relativeFilePath, oldContent, newContent);
    }

    @Tool("修改文件内容，用新内容替换指定的旧内容")
    public String modifyFile(
            @P("文件的相对路径")
            String relativeFilePath,
            @P("要替换的旧内容")
            String oldContent,
            @P("替换后的新内容")
            String newContent,
            @ToolMemoryId Long appId
    ) {
        try {
            Path path = Paths.get(relativeFilePath);
            if (!path.isAbsolute()) {
                String projectDirName = "vue_project_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                path = projectRoot.resolve(relativeFilePath);
            }
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                return "错误：文件不存在或不是文件 - " + relativeFilePath;
            }
            String originalContent = Files.readString(path);
            if (!originalContent.contains(oldContent)) {
                return "警告：文件中未找到要替换的内容，文件未修改 - " + relativeFilePath;
            }
            String modifiedContent = originalContent.replace(oldContent, newContent);
            if (originalContent.equals(modifiedContent)) {
                return "信息：替换后文件内容未发生变化 - " + relativeFilePath;
            }
            Files.writeString(path, modifiedContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("成功修改文件: {}", path.toAbsolutePath());
            return "文件修改成功: " + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "修改文件失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }
}
