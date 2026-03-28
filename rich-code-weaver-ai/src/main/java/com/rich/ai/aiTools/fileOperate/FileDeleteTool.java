package com.rich.ai.aiTools.fileOperate;

import cn.hutool.json.JSONObject;
import com.rich.ai.aiTools.BaseTool;
import com.rich.common.constant.AppConstant;
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
 * 文件删除工具，支持 AI 通过工具调用的方式删除文件
 *
 * @author DuRuiChi
 * @create 2026/1/6
 **/
@Slf4j
@Component
public class FileDeleteTool extends BaseTool {
    @Override
    public String getToolName() {
        return "deleteFile";
    }

    @Override
    public String getToolDisplayName() {
        return "文件删除工具";
    }

    @Override
    public String getResultMsg(JSONObject arguments) {
        // 从参数中提取文件路径，若为空则使用默认值
        String relativeFilePath = arguments.getStr("relativeFilePath");
        String displayPath = (relativeFilePath == null || relativeFilePath.trim().isEmpty())
                ? "未知文件"
                : relativeFilePath;
        return String.format("[工具调用结束] 成功删除文件 %s", displayPath);
    }

    @Tool("删除指定路径的文件")
    public String deleteFile(
            @P("文件的相对路径")
            String relativeFilePath,
            @ToolMemoryId Long appId
    ) {
        // 参数校验：检查文件路径是否为空
        if (relativeFilePath == null || relativeFilePath.trim().isEmpty()) {
            String errorMsg = "错误：文件路径不能为空";
            log.warn(errorMsg);
            return errorMsg;
        }

        // 参数校验：检查应用ID是否有效
        if (appId == null || appId <= 0) {
            String errorMsg = "错误：应用ID无效";
            log.warn(errorMsg);
            return errorMsg;
        }

        try {
            // 解析文件路径
            Path targetPath = Paths.get(relativeFilePath);

            // 如果不是绝对路径，则拼接项目根目录
            if (!targetPath.isAbsolute()) {
                String codeGenType = CreatAndWriteAiTool.APP_CODE_GEN_TYPE_CACHE.getOrDefault(appId, "vue_project");
                String projectDirName = codeGenType + "_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                targetPath = projectRoot.resolve(relativeFilePath);
            }

            // 检查文件是否存在
            if (!Files.exists(targetPath)) {
                String warningMsg = "警告：文件不存在，无需删除 - " + relativeFilePath;
                log.warn(warningMsg);
                return warningMsg;
            }

            // 检查是否为普通文件（非目录）
            if (!Files.isRegularFile(targetPath)) {
                String errorMsg = "错误：指定路径不是文件，无法删除 - " + relativeFilePath;
                log.warn(errorMsg);
                return errorMsg;
            }

            // 安全检查：避免删除重要的项目配置文件
            String fileName = targetPath.getFileName().toString();
            if (isImportantFile(fileName)) {
                String errorMsg = "错误：不允许删除重要文件 - " + fileName;
                log.warn(errorMsg);
                return errorMsg;
            }

            // 执行文件删除操作
            Files.delete(targetPath);
            log.info("成功删除文件: {}", targetPath.toAbsolutePath());
            return "文件删除成功: " + relativeFilePath;

        } catch (IOException e) {
            // 捕获IO异常并记录详细错误信息
            String errorMessage = String.format("删除文件失败: %s, 错误: %s", relativeFilePath, e.getMessage());
            log.error(errorMessage, e);
            return errorMessage;
        } catch (Exception e) {
            // 捕获其他未预期的异常
            String errorMessage = String.format("删除文件时发生未知错误: %s, 错误: %s", relativeFilePath, e.getMessage());
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * 判断是否是重要文件，不允许删除
     * 重要文件包括：项目配置文件、依赖锁定文件、入口文件等
     *
     * @param fileName 文件名
     * @return true-重要文件不可删除, false-普通文件可删除
     */
    private boolean isImportantFile(String fileName) {
        // 参数校验：文件名不能为空
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }

        // 定义重要文件列表（项目核心配置和入口文件）
        String[] importantFiles = {
                // 依赖管理文件
                "package.json", "package-lock.json", "yarn.lock", "pnpm-lock.yaml",
                // 构建配置文件
                "vite.config.js", "vite.config.ts", "vue.config.js",
                // TypeScript配置文件
                "tsconfig.json", "tsconfig.app.json", "tsconfig.node.json",
                // 项目入口文件
                "index.html", "main.js", "main.ts", "App.vue",
                // 版本控制和文档
                ".gitignore", "README.md"
        };

        // 遍历重要文件列表，使用忽略大小写的比较
        for (String importantFileName : importantFiles) {
            if (importantFileName.equalsIgnoreCase(fileName)) {
                return true;
            }
        }

        return false;
    }
}
