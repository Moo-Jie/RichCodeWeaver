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
 * 文件读取工具
 * 支持 AI 通过工具调用的方式读取文件内容
 */
@Slf4j
@Component
public class FileReadTool extends BaseTool {
    @Override
    public String getToolName() {
        return "readFile";
    }

    @Override
    public String getToolDisplayName() {
        return "读取文件工具";
    }

    @Override
    public String getResultMsg(JSONObject arguments) {
        // 从参数中提取文件路径，若为空则使用默认值
        String relativeFilePath = arguments.getStr("relativeFilePath");
        String displayPath = (relativeFilePath == null || relativeFilePath.trim().isEmpty()) 
                ? "未知文件" 
                : relativeFilePath;
        return String.format("[工具调用结束] 成功读取文件 %s", displayPath);
    }

    @Tool("读取指定路径的文件内容")
    public String readFile(@P("文件的相对路径") String relativeFilePath, @ToolMemoryId Long appId) {
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
                String projectDirName = "vue_project_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                targetPath = projectRoot.resolve(relativeFilePath);
            }
            
            // 检查文件是否存在
            if (!Files.exists(targetPath)) {
                String errorMsg = "错误：文件不存在 - " + relativeFilePath;
                log.warn(errorMsg);
                return errorMsg;
            }
            
            // 检查是否为普通文件（非目录）
            if (!Files.isRegularFile(targetPath)) {
                String errorMsg = "错误：指定路径不是文件 - " + relativeFilePath;
                log.warn(errorMsg);
                return errorMsg;
            }
            
            // 读取文件内容并返回
            String fileContent = Files.readString(targetPath);
            log.info("成功读取文件: {}, 大小: {} 字节", targetPath.toAbsolutePath(), fileContent.length());
            return fileContent;
            
        } catch (IOException e) {
            // 捕获IO异常并记录详细错误信息
            String errorMessage = String.format("读取文件失败: %s, 错误: %s", relativeFilePath, e.getMessage());
            log.error(errorMessage, e);
            return errorMessage;
        } catch (Exception e) {
            // 捕获其他未预期的异常
            String errorMessage = String.format("读取文件时发生未知错误: %s, 错误: %s", relativeFilePath, e.getMessage());
            log.error(errorMessage, e);
            return errorMessage;
        }
    }
}
