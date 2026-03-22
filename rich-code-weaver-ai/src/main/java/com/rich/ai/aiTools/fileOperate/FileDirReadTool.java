package com.rich.ai.aiTools.fileOperate;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import com.rich.ai.aiTools.BaseTool;
import com.rich.common.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

/**
 * 文件目录读取工具,支持 AI 通过工具调用的方式读取文件目录
 *
 * @author DuRuiChi
 * @create 2026/1/6
 **/
@Slf4j
@Component
public class FileDirReadTool extends BaseTool {
    /**
     * 需要忽略的文件和目录
     */
    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules", ".git", "dist", "build", ".DS_Store",
            ".env", "target", ".mvn", ".idea", ".vscode", "coverage"
    );
    /**
     * 需要忽略的文件扩展名
     */
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log", ".template", ".cache", ".lock"
    );

    @Override
    public String getToolName() {
        return "readDir";
    }

    @Override
    public String getToolDisplayName() {
        return "文件目录读取工具";
    }

    @Override
    public String getResultMsg(JSONObject arguments) {
        // 从参数中提取目录路径，若为空则使用默认值
        String relativeDirPath = arguments.getStr("relativeDirPath");
        String displayPath = (relativeDirPath == null || relativeDirPath.trim().isEmpty()) 
                ? "项目根目录" 
                : relativeDirPath;
        return String.format("[工具调用结束] 成功读取目录 %s", displayPath);
    }

    @Tool("读取目录结构，获取指定目录下的所有文件和子目录信息")
    public String readDir(
            @P("目录的相对路径，为空则读取整个项目结构")
            String relativeDirPath,
            @ToolMemoryId Long appId
    ) {
        // 参数校验：检查应用ID是否有效
        if (appId == null || appId <= 0) {
            String errorMsg = "错误：应用ID无效";
            log.warn(errorMsg);
            return errorMsg;
        }
        
        try {
            // 处理空路径的情况（读取项目根目录）
            String normalizedPath = (relativeDirPath == null || relativeDirPath.trim().isEmpty()) 
                    ? "" 
                    : relativeDirPath.trim();
            
            // 解析目录路径
            Path targetPath = Paths.get(normalizedPath);
            
            // 如果不是绝对路径，则拼接项目根目录
            if (!targetPath.isAbsolute()) {
                String projectDirName = "vue_project_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                targetPath = projectRoot.resolve(normalizedPath);
            }
            
            // 转换为File对象进行操作
            File targetDir = targetPath.toFile();
            
            // 检查目录是否存在
            if (!targetDir.exists()) {
                String errorMsg = "错误：目录不存在 - " + (normalizedPath.isEmpty() ? "项目根目录" : relativeDirPath);
                log.warn(errorMsg);
                return errorMsg;
            }
            
            // 检查是否为目录（非文件）
            if (!targetDir.isDirectory()) {
                String errorMsg = "错误：指定路径不是目录 - " + relativeDirPath;
                log.warn(errorMsg);
                return errorMsg;
            }
            
            // 构建目录结构字符串
            StringBuilder structure = new StringBuilder();
            structure.append("项目目录结构:\n");
            
            // 使用 Hutool 递归获取所有文件（过滤掉需要忽略的文件）
            List<File> allFiles = FileUtil.loopFiles(targetDir, file -> !shouldIgnore(file.getName()));
            
            // 如果目录为空，返回提示信息
            if (allFiles.isEmpty()) {
                structure.append("（目录为空或所有文件已被过滤）\n");
                log.info("目录为空: {}", targetPath.toAbsolutePath());
                return structure.toString();
            }
            
            // 按路径深度和名称排序显示文件列表
            allFiles.stream()
                    .sorted((file1, file2) -> {
                        // 首先按深度排序（浅层优先）
                        int depth1 = getRelativeDepth(targetDir, file1);
                        int depth2 = getRelativeDepth(targetDir, file2);
                        if (depth1 != depth2) {
                            return Integer.compare(depth1, depth2);
                        }
                        // 深度相同时按路径字典序排序
                        return file1.getPath().compareTo(file2.getPath());
                    })
                    .forEach(file -> {
                        // 计算文件的相对深度，用于缩进显示
                        int depth = getRelativeDepth(targetDir, file);
                        String indent = "  ".repeat(depth);
                        // 添加文件名到结构字符串（每个文件占一行）
                        structure.append(indent).append(file.getName()).append("\n");
                    });
            
            log.info("成功读取目录: {}, 共 {} 个文件", targetPath.toAbsolutePath(), allFiles.size());
            return structure.toString();

        } catch (Exception e) {
            // 捕获所有异常并记录详细错误信息
            String displayPath = (relativeDirPath == null || relativeDirPath.trim().isEmpty()) 
                    ? "项目根目录" 
                    : relativeDirPath;
            String errorMessage = String.format("读取目录结构失败: %s, 错误: %s", displayPath, e.getMessage());
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * 计算文件相对于根目录的深度
     * 深度用于控制目录树的缩进显示
     * 
     * @param rootDir 根目录
     * @param targetFile 目标文件
     * @return 相对深度（0表示根目录下的直接文件）
     */
    private int getRelativeDepth(File rootDir, File targetFile) {
        // 将File转换为Path以便进行路径计算
        Path rootPath = rootDir.toPath();
        Path filePath = targetFile.toPath();
        
        // 计算相对路径的层级数，减1是因为文件本身也算一层
        // 例如：root/a/b.txt 相对于 root 的深度为 1（在a目录下）
        return rootPath.relativize(filePath).getNameCount() - 1;
    }

    /**
     * 判断是否应该忽略该文件或目录
     * 忽略规则：1. 匹配忽略名称列表  2. 匹配忽略扩展名列表
     * 
     * @param fileName 文件或目录名称
     * @return true-应该忽略, false-不应该忽略
     */
    private boolean shouldIgnore(String fileName) {
        // 参数校验：文件名为空则不忽略
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }
        
        // 检查是否在忽略名称列表中（精确匹配）
        if (IGNORED_NAMES.contains(fileName)) {
            return true;
        }

        // 检查文件扩展名是否在忽略列表中（后缀匹配）
        return IGNORED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }
}
