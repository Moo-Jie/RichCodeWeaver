package com.rich.app.utils.codeConcatenate;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 代码拼接工具类
 * 拼接代码目录下的源码文件用于 AI 审查，并统一排除依赖、构建产物和超大锁文件
 *
 * @author DuRuiChi
 * @create 2026/1/18
 **/
@Slf4j
public class CodeConcatenateUtiles {
    /**
     * 需要检查的文件扩展名
     */
    private static final List<String> CODE_EXTENSIONS = Arrays.asList(
            ".html", ".htm", ".css", ".js", ".mjs", ".cjs", ".json", ".vue", ".ts", ".jsx", ".tsx"
    );

    /**
     * 不参与 AI 审查的目录
     */
    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            "node_modules", "dist", "build", "output", "target", ".git", ".idea", ".vscode", "coverage"
    );

    /**
     * 不参与 AI 审查的锁文件和构建元数据
     */
    private static final Set<String> IGNORED_FILE_NAMES = Set.of(
            "package-lock.json", "yarn.lock", "pnpm-lock.yaml", ".env", ".ds_store"
    );

    /**
     * 单次 AI 审查允许拼接的最大字符数
     */
    private static final int MAX_TOTAL_CHARACTERS = 500_000;

    /**
     * 读取并拼接代码目录下的所有源码文件
     *
     * @param codeDir 需要扫描的代码目录路径
     * @return 拼接后的完整代码内容（Markdown 格式）
     */
    public static String readAndConcatenateCodeFiles(String codeDir) {
        if (StrUtil.isBlank(codeDir)) {
            return "";
        }

        File directory = new File(codeDir);
        if (!directory.exists() || !directory.isDirectory()) {
            log.error("代码目录不存在或不是目录: {}", codeDir);
            return "";
        }

        StringBuilder codeContent = new StringBuilder();
        codeContent.append("# 项目文件结构和代码内容\n\n");

        FileUtil.walkFiles(directory, file -> {
            if (codeContent.length() >= MAX_TOTAL_CHARACTERS || skipFile(file, directory)) {
                return;
            }
            if (!isCodeFile(file)) {
                return;
            }

            String relativePath = normalizeRelativePath(FileUtil.subPath(
                    directory.getAbsolutePath(), file.getAbsolutePath()));
            codeContent.append("## 文件: ").append(relativePath).append("\n\n");
            String fileContent = FileUtil.readUtf8String(file);
            codeContent.append(fileContent).append("\n\n");
        });

        if (codeContent.length() >= MAX_TOTAL_CHARACTERS) {
            codeContent.append("\n> [系统提示] 审查输入已达到 ")
                    .append(MAX_TOTAL_CHARACTERS)
                    .append(" 字符上限，后续文件未拼接；最终仍以 npm run build 结果作为构建门禁。\n");
        }
        return codeContent.toString();
    }

    /**
     * 文件过滤逻辑
     *
     * @param file    当前检查的文件对象
     * @param rootDir 项目根目录
     * @return true 表示需要跳过该文件
     */
    private static boolean skipFile(File file, File rootDir) {
        String normalizedName = file.getName().toLowerCase(Locale.ROOT);
        if (file.getName().startsWith(".") || IGNORED_FILE_NAMES.contains(normalizedName)) {
            return true;
        }

        // Hutool 在 Windows 上返回正斜杠，必须统一分隔符后再判断目录
        String relativePath = normalizeRelativePath(FileUtil.subPath(
                rootDir.getAbsolutePath(), file.getAbsolutePath()));
        return IGNORED_DIRECTORIES.stream()
                .anyMatch(directoryName -> relativePath.equals(directoryName)
                        || relativePath.startsWith(directoryName + "/"));
    }

    /**
     * 判断是否为需要审查的代码文件
     *
     * @param file 文件
     * @return true 表示源码文件
     */
    private static boolean isCodeFile(File file) {
        String fileName = file.getName().toLowerCase(Locale.ROOT);
        return CODE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    /**
     * 统一相对路径分隔符
     *
     * @param path 相对路径
     * @return 正斜杠路径
     */
    private static String normalizeRelativePath(String path) {
        return path == null ? "" : path.replace('\\', '/');
    }
}
