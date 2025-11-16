package com.rich.app.utils.codeConcatenate;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * 代码拼接工具类
 * 拼接代码目录下的所有代码文件,用于交给 AI 进行审查
 *
 * @author DuRuiChi
 * @create 2025/9/18
 **/
@Slf4j
public class CodeConcatenateUtiles {
    /**
     * 需要检查的文件扩展名
     */
    private static final List<String> CODE_EXTENSIONS = Arrays.asList(
            ".html", ".htm", ".css", ".js", ".json", ".vue", ".ts", ".jsx", ".tsx"
    );

    /**
     * 读取并拼接代码目录下的所有代码文件
     *
     * @param codeDir 需要扫描的代码目录路径
     * @return 拼接后的完整代码内容（Markdown格式）
     */
    public static String readAndConcatenateCodeFiles(String codeDir) {
        // 检查空目录路径
        if (StrUtil.isBlank(codeDir)) {
            return "";
        }

        // 创建目录对象并验证有效性
        File directory = new File(codeDir);
        if (!directory.exists() || !directory.isDirectory()) {
            log.error("代码目录不存在或不是目录: {}", codeDir);
            return "";
        }

        StringBuilder codeContent = new StringBuilder();
        codeContent.append("# 项目文件结构和代码内容\n\n");

        // 使用 Hutool 文件遍历方法处理每个文件
        FileUtil.walkFiles(directory, file -> {
            // 跳过隐藏文件、依赖目录和非代码文件
            if (skipFile(file, directory)) {
                return;
            }

            // 确认是代码文件后处理内容
            if (isCodeFile(file)) {
                // 获取相对路径用于展示
                String relativePath = FileUtil.subPath(directory.getAbsolutePath(), file.getAbsolutePath());
                codeContent.append("## 文件: ").append(relativePath).append("\n\n");

                // 读取文件内容并追加到结果
                String fileContent = FileUtil.readUtf8String(file);
                codeContent.append(fileContent).append("\n\n");
            }
        });

        return codeContent.toString();
    }


    /**
     * 文件过滤逻辑
     *
     * @param file    当前检查的文件对象
     * @param rootDir 项目根目录
     * @return true表示需要跳过该文件
     */
    private static boolean skipFile(File file, File rootDir) {
        // 获取相对于根目录的路径
        String relativePath = FileUtil.subPath(rootDir.getAbsolutePath(), file.getAbsolutePath());

        // 跳过隐藏文件（以.开头的文件/目录）
        if (file.getName().startsWith(".")) {
            return true;
        }

        // 跳过常见依赖目录
        return relativePath.contains("node_modules" + File.separator)
                || relativePath.contains("dist" + File.separator)
                || relativePath.contains("target" + File.separator)
                || relativePath.contains(".git" + File.separator);
    }


    /**
     * 判断是否是需要检查的代码文件
     */
    private static boolean isCodeFile(File file) {
        String fileName = file.getName().toLowerCase();
        return CODE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

}
