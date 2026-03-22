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
import java.nio.file.StandardOpenOption;

/**
 * 文件修改工具,支持 AI 通过工具调用的方式修改文件内容
 *
 * @author DuRuiChi
 * @create 2026/1/6
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
        // 从参数中提取文件路径，若为空则使用默认值
        String relativeFilePath = arguments.getStr("relativeFilePath");
        String displayPath = (relativeFilePath == null || relativeFilePath.trim().isEmpty()) 
                ? "未知文件" 
                : relativeFilePath;
        return String.format("[工具调用结束] 成功修改文件 %s", displayPath);
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
        // 参数校验：检查文件路径是否为空
        if (relativeFilePath == null || relativeFilePath.trim().isEmpty()) {
            String errorMsg = "错误：文件路径不能为空";
            log.warn(errorMsg);
            return errorMsg;
        }
        
        // 参数校验：检查旧内容是否为空（允许空字符串，但不允许null）
        if (oldContent == null) {
            String errorMsg = "错误：要替换的旧内容不能为null";
            log.warn(errorMsg);
            return errorMsg;
        }
        
        // 参数校验：检查新内容是否为空（允许空字符串，但不允许null）
        if (newContent == null) {
            String errorMsg = "错误：替换后的新内容不能为null";
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
            
            // 读取原始文件内容
            String originalContent = Files.readString(targetPath);
            
            // 检查文件中是否包含要替换的旧内容
            if (!originalContent.contains(oldContent)) {
                String warningMsg = "警告：文件中未找到要替换的内容，文件未修改 - " + relativeFilePath;
                log.warn(warningMsg);
                return warningMsg;
            }
            
            // 执行内容替换（替换所有匹配项）
            String modifiedContent = originalContent.replace(oldContent, newContent);
            
            // 检查替换后内容是否发生变化
            if (originalContent.equals(modifiedContent)) {
                String infoMsg = "信息：替换后文件内容未发生变化 - " + relativeFilePath;
                log.info(infoMsg);
                return infoMsg;
            }
            
            // 将修改后的内容写回文件（覆盖原文件）
            Files.writeString(targetPath, modifiedContent, 
                    StandardOpenOption.CREATE, 
                    StandardOpenOption.TRUNCATE_EXISTING);
            
            log.info("成功修改文件: {}, 替换了 {} 处内容", 
                    targetPath.toAbsolutePath(), 
                    countOccurrences(originalContent, oldContent));
            return "文件修改成功: " + relativeFilePath;
            
        } catch (IOException e) {
            // 捕获IO异常并记录详细错误信息
            String errorMessage = String.format("修改文件失败: %s, 错误: %s", relativeFilePath, e.getMessage());
            log.error(errorMessage, e);
            return errorMessage;
        } catch (Exception e) {
            // 捕获其他未预期的异常
            String errorMessage = String.format("修改文件时发生未知错误: %s, 错误: %s", relativeFilePath, e.getMessage());
            log.error(errorMessage, e);
            return errorMessage;
        }
    }
    
    /**
     * 统计字符串中子串出现的次数
     * 
     * @param content 原始内容
     * @param substring 要查找的子串
     * @return 出现次数
     */
    private int countOccurrences(String content, String substring) {
        if (content == null || substring == null || substring.isEmpty()) {
            return 0;
        }
        
        int count = 0;
        int index = 0;
        
        // 循环查找子串出现的位置
        while ((index = content.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        
        return count;
    }
}
