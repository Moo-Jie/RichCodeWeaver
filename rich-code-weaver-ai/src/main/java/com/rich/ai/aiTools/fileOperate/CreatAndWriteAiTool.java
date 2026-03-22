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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 文件创建并写入工具，支持 AI 通过工具调用的创建并写入文件
 *
 * @author DuRuiChi
 * @create 2025/12/24
 **/
@Slf4j
@Component
public class CreatAndWriteAiTool extends BaseTool {
    @Override
    public String getToolName() {
        return "creatAndWrite";
    }

    @Override
    public String getToolDisplayName() {
        return "文件创建并写入工具";
    }

    @Override
    public String getResultMsg(JSONObject arguments) {
        // 从参数中提取文件路径，若为空则使用默认值
        String relativeFilePath = arguments.getStr("relativeFilePath");
        String displayPath = (relativeFilePath == null || relativeFilePath.trim().isEmpty()) 
                ? "未知文件" 
                : relativeFilePath;
        return String.format("[工具调用结束] 成功创建文件 %s", displayPath);
    }


    /**
     * 生成文件到指定路径
     * 如果文件已存在则覆盖，如果父目录不存在则自动创建
     *
     * @param relativeFilePath 文件的相对路径
     * @param content          要写入文件的内容
     * @param appId            应用ID，用于定位项目根目录
     * @return 写入结果消息
     */
    @Tool("生成文件到指定路径")
    public String creatAndWrite(
            @P("需要生成的文件的相对路径") String relativeFilePath, 
            @P("需要写入文件的内容") String content, 
            @ToolMemoryId Long appId) {
        
        // 参数校验：检查文件路径是否为空
        if (relativeFilePath == null || relativeFilePath.trim().isEmpty()) {
            String errorMsg = "错误：文件路径不能为空";
            log.warn(errorMsg);
            return errorMsg;
        }
        
        // 参数校验：检查内容是否为null（允许空字符串）
        if (content == null) {
            String errorMsg = "错误：文件内容不能为null";
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
            
            // 获取父目录路径
            Path parentDir = targetPath.getParent();
            
            // 如果父目录不存在，则递归创建所有必需的父目录
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                log.info("创建父目录: {}", parentDir.toAbsolutePath());
            }
            
            // 将内容写入文件（如果文件已存在则覆盖）
            // 使用UTF-8编码将字符串转换为字节数组
            Files.write(targetPath, content.getBytes(java.nio.charset.StandardCharsets.UTF_8), 
                    StandardOpenOption.CREATE, 
                    StandardOpenOption.TRUNCATE_EXISTING);
            
            log.info("成功创建并写入文件: {}, 大小: {} 字节", 
                    targetPath.toAbsolutePath(), 
                    content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
            
            return "文件创建并写入成功: " + relativeFilePath;
            
        } catch (IOException e) {
            // 捕获IO异常并记录详细错误信息
            String errorMessage = String.format("文件创建并写入失败: %s, 错误: %s", relativeFilePath, e.getMessage());
            log.error(errorMessage, e);
            return errorMessage;
        } catch (Exception e) {
            // 捕获其他未预期的异常
            String errorMessage = String.format("创建文件时发生未知错误: %s, 错误: %s", relativeFilePath, e.getMessage());
            log.error(errorMessage, e);
            return errorMessage;
        }
    }
}
