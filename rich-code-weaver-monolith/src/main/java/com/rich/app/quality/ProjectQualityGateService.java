package com.rich.app.quality;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rich.model.enums.CodeGeneratorTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 产物质量门禁服务
 * 负责在执行真实构建前检查项目结构和本地资源引用，避免明显错误进入 npm 构建阶段
 *
 * @author DuRuiChi
 * @create 2026/11/6
 **/
@Slf4j
@Service
public class ProjectQualityGateService {

    /**
     * 需要检查 import 的源码扩展名
     */
    private static final Set<String> SOURCE_EXTENSIONS = Set.of(
            ".vue", ".js", ".mjs", ".cjs", ".ts", ".tsx", ".jsx", ".css", ".scss", ".less"
    );

    /**
     * 构建时需要跳过的目录
     */
    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            "node_modules", "dist", "build", ".git", ".idea", ".vscode", "target"
    );

    /**
     * 本地 import 匹配规则
     */
    private static final Pattern IMPORT_PATTERN = Pattern.compile(
            "(?:import\\s+(?:[^'\"\\n]+?\\s+from\\s+)?|export\\s+[^'\"\\n]+?\\s+from\\s+)[\"']([^\"']+)[\"']"
    );

    /**
     * HTML script 引用匹配规则
     */
    private static final Pattern SCRIPT_SRC_PATTERN = Pattern.compile(
            "<script[^>]+src\\s*=\\s*[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * HTML link 引用匹配规则
     */
    private static final Pattern LINK_HREF_PATTERN = Pattern.compile(
            "<link[^>]+href\\s*=\\s*[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * CSS url 引用匹配规则
     */
    private static final Pattern CSS_URL_PATTERN = Pattern.compile(
            "url\\(\\s*[\"']?([^\"')]+)[\"']?\\s*\\)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Vite base 配置匹配规则
     */
    private static final Pattern VITE_BASE_PATTERN = Pattern.compile(
            "base\\s*:\\s*[\"']\\./[\"']"
    );

    /**
     * 校验产物是否满足可交付条件
     *
     * @param outputDir      产物目录
     * @param generationType 代码生成类型
     * @return 校验结果
     */
    public ValidationResult validate(String outputDir, CodeGeneratorTypeEnum generationType) {
        List<String> errors = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        if (StrUtil.isBlank(outputDir)) {
            return ValidationResult.failed(List.of("代码输出目录为空"), suggestions);
        }
        if (generationType == null) {
            return ValidationResult.failed(List.of("代码生成类型为空"), suggestions);
        }

        File projectDir = new File(outputDir);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            return ValidationResult.failed(List.of("代码输出目录不存在: " + outputDir), suggestions);
        }

        switch (generationType) {
            case VUE_PROJECT -> validateVueProject(projectDir, errors, suggestions);
            case MULTI_FILE -> validateMultiFileProject(projectDir, errors, suggestions);
            case HTML -> validateHtmlProject(projectDir, errors, suggestions);
            default -> errors.add("当前生成类型不支持质量门禁: " + generationType.getValue());
        }

        if (!errors.isEmpty()) {
            log.warn("产物质量门禁未通过: outputDir={}, type={}, errors={}",
                    outputDir, generationType.getValue(), errors);
        }
        return errors.isEmpty()
                ? ValidationResult.passed(suggestions)
                : ValidationResult.failed(errors, suggestions);
    }

    /**
     * 校验 Vue 工程结构、依赖声明和本地资源引用
     *
     * @param projectDir  项目目录
     * @param errors      错误列表
     * @param suggestions 建议列表
     */
    private void validateVueProject(File projectDir, List<String> errors, List<String> suggestions) {
        checkRequiredFile(projectDir, "index.html", "缺少入口文件 index.html（Vite 构建必需）", errors);
        checkRequiredFile(projectDir, "package.json", "缺少 package.json（无法安装依赖和执行构建）", errors);
        checkRequiredFile(projectDir, "src/main.js", "缺少 src/main.js（Vue 入口文件）", errors);
        checkRequiredFile(projectDir, "src/App.vue", "缺少 src/App.vue（Vue 根组件）", errors);

        File viteConfig = firstExistingFile(projectDir, "vite.config.js", "vite.config.mjs", "vite.config.ts");
        if (viteConfig == null) {
            errors.add("缺少 vite.config.js（Vite 构建配置）");
        }

        File indexHtml = new File(projectDir, "index.html");
        if (indexHtml.isFile()) {
            String html = readFileQuietly(indexHtml);
            if (!html.contains("src/main.js") && !html.contains("src/main.ts")) {
                errors.add("index.html 未引用 src/main.js 或 src/main.ts");
            }
        }

        File packageJson = new File(projectDir, "package.json");
        if (packageJson.isFile()) {
            validatePackageJson(packageJson, errors);
        }

        if (viteConfig != null) {
            String viteConfigContent = readFileQuietly(viteConfig);
            if (!VITE_BASE_PATTERN.matcher(viteConfigContent).find()) {
                errors.add("vite.config 未配置 base: './'，子路径部署时静态资源可能 404");
            }
            if (!viteConfigContent.contains("vue()")) {
                errors.add("vite.config 未启用 @vitejs/plugin-vue 的 vue() 插件");
            }
        }

        File routerFile = firstExistingFile(projectDir, "src/router/index.js", "src/router/index.ts");
        if (routerFile != null) {
            String routerContent = readFileQuietly(routerFile);
            if (!routerContent.contains("createWebHashHistory")) {
                errors.add("Vue 路由未使用 createWebHashHistory，静态子路径部署会失败");
            }
        }

        validateLocalReferences(projectDir, errors);
        suggestions.add("如果质量门禁仍失败，请优先读取错误中提到的文件，再做局部修复");
        suggestions.add("Vue 项目必须以 npm run build 的真实退出码作为最终构建结论");
    }

    /**
     * 校验多文件静态页面结构和本地资源引用
     *
     * @param projectDir  项目目录
     * @param errors      错误列表
     * @param suggestions 建议列表
     */
    private void validateMultiFileProject(File projectDir, List<String> errors, List<String> suggestions) {
        checkRequiredFile(projectDir, "index.html", "缺少 index.html 主文档文件", errors);
        checkRequiredFile(projectDir, "style.css", "缺少 style.css 样式文件", errors);
        checkRequiredFile(projectDir, "script.js", "缺少 script.js 脚本文件", errors);
        validateHtmlReferences(projectDir, new File(projectDir, "index.html"), errors);
        validateLocalReferences(projectDir, errors);
        suggestions.add("多文件产物至少需要 index.html、style.css、script.js，且引用路径必须存在");
    }

    /**
     * 校验单 HTML 页面
     *
     * @param projectDir  项目目录
     * @param errors      错误列表
     * @param suggestions 建议列表
     */
    private void validateHtmlProject(File projectDir, List<String> errors, List<String> suggestions) {
        File[] htmlFiles = projectDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".html"));
        if (htmlFiles == null || htmlFiles.length == 0) {
            errors.add("输出目录中未找到任何 .html 文件");
            return;
        }
        for (File htmlFile : htmlFiles) {
            if (htmlFile.length() == 0) {
                errors.add("HTML 文件为空: " + htmlFile.getName());
                continue;
            }
            validateHtmlReferences(projectDir, htmlFile, errors);
        }
        suggestions.add("单 HTML 页面必须能直接在浏览器打开，避免依赖不存在的本地资源");
    }

    /**
     * 校验 package.json 中的构建脚本和核心依赖
     *
     * @param packageJson package.json 文件
     * @param errors      错误列表
     */
    private void validatePackageJson(File packageJson, List<String> errors) {
        try {
            JSONObject packageObject = JSONUtil.parseObj(Files.readString(packageJson.toPath()));
            JSONObject scripts = packageObject.getJSONObject("scripts");
            if (scripts == null || StrUtil.isBlank(scripts.getStr("build"))) {
                errors.add("package.json 缺少 scripts.build，无法执行 npm run build");
            }
            Set<String> dependencies = new HashSet<>();
            collectDependencyNames(packageObject.getJSONObject("dependencies"), dependencies);
            collectDependencyNames(packageObject.getJSONObject("devDependencies"), dependencies);
            if (!dependencies.contains("vue")) {
                errors.add("package.json 缺少 vue 依赖");
            }
            if (!dependencies.contains("vite")) {
                errors.add("package.json 缺少 vite 开发依赖");
            }
            if (!dependencies.contains("@vitejs/plugin-vue")) {
                errors.add("package.json 缺少 @vitejs/plugin-vue 开发依赖");
            }
        } catch (Exception e) {
            errors.add("package.json 不是合法 JSON: " + e.getMessage());
        }
    }

    /**
     * 收集依赖名称
     *
     * @param dependencyObject dependencies 或 devDependencies 对象
     * @param dependencyNames  依赖名称集合
     */
    private void collectDependencyNames(JSONObject dependencyObject, Set<String> dependencyNames) {
        if (dependencyObject != null) {
            dependencyNames.addAll(dependencyObject.keySet());
        }
    }

    /**
     * 校验源码中引用的本地文件是否存在
     *
     * @param projectDir 项目目录
     * @param errors     错误列表
     */
    private void validateLocalReferences(File projectDir, List<String> errors) {
        List<File> sourceFiles = new ArrayList<>();
        collectSourceFiles(projectDir, sourceFiles);
        Set<String> reportedPaths = new HashSet<>();
        for (File sourceFile : sourceFiles) {
            String content = readFileQuietly(sourceFile);
            if (StrUtil.isBlank(content)) {
                continue;
            }
            if (sourceFile.getName().toLowerCase(Locale.ROOT).endsWith(".html")) {
                validateHtmlReferences(projectDir, sourceFile, errors);
            }
            validateImportReferences(projectDir, sourceFile, content, errors, reportedPaths);
            validateCssReferences(projectDir, sourceFile, content, errors, reportedPaths);
        }
    }

    /**
     * 校验 import/export 中引用的本地文件
     *
     * @param projectDir     项目目录
     * @param sourceFile     源文件
     * @param content        文件内容
     * @param errors         错误列表
     * @param reportedPaths  已报告路径集合
     */
    private void validateImportReferences(File projectDir, File sourceFile, String content,
                                          List<String> errors, Set<String> reportedPaths) {
        Matcher matcher = IMPORT_PATTERN.matcher(content);
        while (matcher.find()) {
            String importPath = matcher.group(1);
            if (!isLocalReference(importPath)) {
                continue;
            }
            File targetFile = resolveLocalReference(projectDir, sourceFile, importPath);
            if (!targetFile.exists() && reportedPaths.add(sourceFile.getPath() + "|" + importPath)) {
                errors.add(buildReferenceError(projectDir, sourceFile, importPath));
            }
        }
    }

    /**
     * 校验 HTML 中 script/link 引用的本地文件
     *
     * @param projectDir 项目目录
     * @param htmlFile   HTML 文件
     * @param errors     错误列表
     */
    private void validateHtmlReferences(File projectDir, File htmlFile, List<String> errors) {
        if (!htmlFile.isFile()) {
            return;
        }
        String content = readFileQuietly(htmlFile);
        validateHtmlPattern(projectDir, htmlFile, content, SCRIPT_SRC_PATTERN, errors);
        validateHtmlPattern(projectDir, htmlFile, content, LINK_HREF_PATTERN, errors);
    }

    /**
     * 校验 HTML 引用规则
     *
     * @param projectDir 项目目录
     * @param htmlFile   HTML 文件
     * @param content    文件内容
     * @param pattern    匹配规则
     * @param errors     错误列表
     */
    private void validateHtmlPattern(File projectDir, File htmlFile, String content,
                                     Pattern pattern, List<String> errors) {
        Matcher matcher = pattern.matcher(content);
        Set<String> reportedPaths = new HashSet<>();
        while (matcher.find()) {
            String referencePath = matcher.group(1);
            if (!isLocalReference(referencePath)) {
                continue;
            }
            File targetFile = resolveLocalReference(projectDir, htmlFile, referencePath);
            if (!targetFile.exists() && reportedPaths.add(referencePath)) {
                errors.add(buildReferenceError(projectDir, htmlFile, referencePath));
            }
        }
    }

    /**
     * 校验 CSS 中 url() 引用的本地文件
     *
     * @param projectDir    项目目录
     * @param sourceFile    源文件
     * @param content       文件内容
     * @param errors        错误列表
     * @param reportedPaths 已报告路径集合
     */
    private void validateCssReferences(File projectDir, File sourceFile, String content,
                                       List<String> errors, Set<String> reportedPaths) {
        if (!sourceFile.getName().toLowerCase(Locale.ROOT).endsWith(".css")) {
            return;
        }
        Matcher matcher = CSS_URL_PATTERN.matcher(content);
        while (matcher.find()) {
            String referencePath = matcher.group(1);
            if (!isLocalReference(referencePath)) {
                continue;
            }
            File targetFile = resolveLocalReference(projectDir, sourceFile, referencePath);
            if (!targetFile.exists() && reportedPaths.add(sourceFile.getPath() + "|" + referencePath)) {
                errors.add(buildReferenceError(projectDir, sourceFile, referencePath));
            }
        }
    }

    /**
     * 判断是否为需要落到本地文件系统的引用
     *
     * @param referencePath 引用路径
     * @return true 表示本地引用
     */
    private boolean isLocalReference(String referencePath) {
        if (StrUtil.isBlank(referencePath)) {
            return false;
        }
        String normalized = referencePath.trim();
        if (normalized.startsWith("#") || normalized.startsWith("data:")
                || normalized.startsWith("http://") || normalized.startsWith("https://")
                || normalized.startsWith("//") || normalized.startsWith("node:")) {
            return false;
        }
        return normalized.startsWith("./") || normalized.startsWith("../")
                || normalized.startsWith("@/") || normalized.startsWith("/src/");
    }

    /**
     * 解析本地引用为实际文件
     *
     * @param projectDir    项目目录
     * @param sourceFile    源文件
     * @param referencePath 引用路径
     * @return 解析后的文件
     */
    private File resolveLocalReference(File projectDir, File sourceFile, String referencePath) {
        String cleanPath = referencePath.split("[?#]", 2)[0];
        if (cleanPath.startsWith("@/")) {
            return withExtensionFallback(new File(projectDir, "src/" + cleanPath.substring(2)));
        }
        if (cleanPath.startsWith("/src/")) {
            return withExtensionFallback(new File(projectDir, cleanPath.substring(1)));
        }
        if (cleanPath.startsWith("/")) {
            return withExtensionFallback(new File(projectDir, cleanPath.substring(1)));
        }
        File parentDir = sourceFile.getParentFile();
        return withExtensionFallback(new File(parentDir, cleanPath));
    }

    /**
     * 对省略扩展名的引用尝试常见源码扩展名
     *
     * @param targetFile 目标文件
     * @return 可命中的文件
     */
    private File withExtensionFallback(File targetFile) {
        File normalizedFile = targetFile.toPath().normalize().toFile();
        if (normalizedFile.exists()) {
            return normalizedFile;
        }
        String fileName = normalizedFile.getName();
        if (!fileName.contains(".")) {
            for (String extension : SOURCE_EXTENSIONS) {
                File candidate = new File(normalizedFile.getParentFile(), fileName + extension);
                if (candidate.exists()) {
                    return candidate;
                }
            }
            for (String extension : SOURCE_EXTENSIONS) {
                File candidate = new File(normalizedFile, "index" + extension);
                if (candidate.exists()) {
                    return candidate;
                }
            }
        }
        return normalizedFile;
    }

    /**
     * 构建资源缺失错误信息
     *
     * @param projectDir    项目目录
     * @param sourceFile    源文件
     * @param referencePath 引用路径
     * @return 错误信息
     */
    private String buildReferenceError(File projectDir, File sourceFile, String referencePath) {
        String relativePath = projectDir.toPath().relativize(sourceFile.toPath()).toString().replace('\\', '/');
        return "文件 " + relativePath + " 引用了不存在的本地资源: " + referencePath;
    }

    /**
     * 递归收集需要检查的源码文件
     *
     * @param dir     当前目录
     * @param results 文件结果列表
     */
    private void collectSourceFiles(File dir, List<File> results) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                if (!IGNORED_DIRECTORIES.contains(file.getName())) {
                    collectSourceFiles(file, results);
                }
                continue;
            }
            String fileName = file.getName().toLowerCase(Locale.ROOT);
            if (SOURCE_EXTENSIONS.stream().anyMatch(fileName::endsWith) || fileName.endsWith(".html")) {
                results.add(file);
            }
        }
    }

    /**
     * 检查必须文件是否存在
     *
     * @param projectDir 项目目录
     * @param path       相对路径
     * @param message    错误信息
     * @param errors     错误列表
     */
    private void checkRequiredFile(File projectDir, String path, String message, List<String> errors) {
        if (!new File(projectDir, path).isFile()) {
            errors.add(message);
        }
    }

    /**
     * 返回第一个存在的文件
     *
     * @param projectDir 项目目录
     * @param paths      候选相对路径
     * @return 存在的文件，不存在返回 null
     */
    private File firstExistingFile(File projectDir, String... paths) {
        for (String path : paths) {
            File file = new File(projectDir, path);
            if (file.isFile()) {
                return file;
            }
        }
        return null;
    }

    /**
     * 安静读取 UTF-8 文件
     *
     * @param file 文件
     * @return 文件内容，读取失败返回空字符串
     */
    private String readFileQuietly(File file) {
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("读取质量门禁文件失败: {}, error={}", file.getAbsolutePath(), e.getMessage());
            return "";
        }
    }

    /**
     * 质量门禁校验结果
     *
     * @param passed      是否通过
     * @param errors      阻断错误
     * @param suggestions 修复建议
     */
    public record ValidationResult(boolean passed, List<String> errors, List<String> suggestions) {

        /**
         * 构建通过结果
         *
         * @param suggestions 修复建议
         * @return 通过结果
         */
        public static ValidationResult passed(List<String> suggestions) {
            return new ValidationResult(true, List.of(), List.copyOf(suggestions));
        }

        /**
         * 构建失败结果
         *
         * @param errors      阻断错误
         * @param suggestions 修复建议
         * @return 失败结果
         */
        public static ValidationResult failed(List<String> errors, List<String> suggestions) {
            return new ValidationResult(false, List.copyOf(errors), List.copyOf(suggestions));
        }
    }
}
