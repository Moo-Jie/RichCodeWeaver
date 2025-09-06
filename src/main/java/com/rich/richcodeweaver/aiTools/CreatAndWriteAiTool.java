package com.rich.richcodeweaver.aiTools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
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
 * 文件创建并写入工具，支持 AI 通过工具调用的创建并写入文件
 *
 * @author DuRuiChi
 * @create 2025/8/24
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
        String relativeFilePath = arguments.getStr("relativeFilePath");
        String suffix = FileUtil.getSuffix(relativeFilePath);
        String content = arguments.getStr("content");
        relativeFilePath = relativeFilePath == null || relativeFilePath.isEmpty() ? "" : "\n[\n" + relativeFilePath + "\n]\n";
        return String.format("""
                [工具调用结束]  %s %s
                ```%s
                %s
                ```
                """, "成功创建并写入以下文件", relativeFilePath, suffix, content);
    }


    /**
     * 生成文件到指定路径
     *
     * @param relativeFilePath 文件的相对路径
     * @param content          要写入文件的内容
     * @param appId            应用 ID ,用于让 AI 主动唯一标识正在生成的应用
     * @return 写入结果
     */
    @Tool("生成文件到指定路径")
    public String creatAndWrite(@P("需要生成的文件的相对路径") String relativeFilePath, @P("需要要写入文件的内容") String content, @ToolMemoryId Long appId) {
        try {
            // 拼接为绝对路径
            Path path = Paths.get(relativeFilePath);
            if (!path.isAbsolute()) {
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, "vue_project_" + appId);
                path = projectRoot.resolve(relativeFilePath);
            }
            // 创建父目录
            Path parentDir = path.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            // 写入文件内容
            Files.write(path, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("成功写入文件: {}", path.toAbsolutePath());
            // 返回相对路径
            return "文件创建并写入成功: " + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "文件创建并写入失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }
}
