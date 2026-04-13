package com.rich.codeweaver.langGraph.node;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.rich.codeweaver.langGraph.state.WorkflowContext;
import com.rich.codeweaver.model.dto.generator.CodeReviewResponse;
import com.rich.codeweaver.model.enums.CodeGeneratorTypeEnum;
import com.rich.codeweaver.service.generator.AiCodeReviewService;
import com.rich.codeweaver.common.utils.SpringContextUtil;
import com.rich.codeweaver.utils.codeConcatenate.CodeConcatenateUtiles;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 代码审查节点
 *
 * @author DuRuiChi
 * @create 2026/1/18
 **/
@Slf4j
public class AICodeReviewNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("\n 正在执行节点: 代码审查节点。 \n");
            String outputDir = context.getOutputDir();
            CodeGeneratorTypeEnum generationType = context.getGenerationType();
            CodeReviewResponse codeReviewResponse;
            try {
                // ====== 结构性预检查：检查关键文件是否存在（AI 审查无法检测磁盘文件缺失） ======
                List<String> structuralErrors = checkProjectStructure(outputDir, generationType);
                if (!structuralErrors.isEmpty()) {
                    log.warn("项目结构检查未通过，发现 {} 个结构性错误", structuralErrors.size());
                    codeReviewResponse = CodeReviewResponse.builder()
                            .isPass(false)
                            .errorList(structuralErrors)
                            .suggestionList(buildStructuralSuggestions(generationType))
                            .build();
                } else {
                    // ====== AI 代码审查 ======
                    String codeContent = CodeConcatenateUtiles.readAndConcatenateCodeFiles(outputDir);
                    if (StrUtil.isBlank(codeContent)) {
                        log.warn("未找到可审查的代码文件");
                        codeReviewResponse = CodeReviewResponse.builder()
                                .isPass(false)
                                .errorList(List.of("未找到可审查的代码文件"))
                                .suggestionList(List.of("请确保代码生成成功"))
                                .build();
                    } else {
                        AiCodeReviewService aiCodeReviewService = SpringContextUtil.getBean(AiCodeReviewService.class);
                        codeReviewResponse = aiCodeReviewService.codeReview(codeContent);
                        if (codeReviewResponse.getIsPass()) {
                            log.info("\n 代码审查通过。\n");
                        } else {
                            log.info("\n 代码审查未通过,错误列表: {}\n", codeReviewResponse.getErrorList());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("代码审查异常: {}", e.getMessage(), e);
                codeReviewResponse = CodeReviewResponse.builder()
                        .isPass(true) // 异常直接跳到下一个步骤
                        .build();
            }
            // 3. 更新状态
            Long currentCount = codeReviewResponse.getReviewCount();
            codeReviewResponse.setReviewCount(currentCount != null ? currentCount + 1 : 1L);
            context.setCurrentStep("代码审查");
            context.setCodeReviewResponse(codeReviewResponse);
            log.info("\n 代码审查节点运行完成。\n");
            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * 检查项目结构是否完整（磁盘文件级别的检查，AI 审查无法检测）
     *
     * @param outputDir      生成代码的输出目录
     * @param generationType 代码生成类型
     * @return 结构性错误列表，空则表示检查通过
     */
    private static List<String> checkProjectStructure(String outputDir, CodeGeneratorTypeEnum generationType) {
        List<String> errors = new ArrayList<>();
        if (StrUtil.isBlank(outputDir)) {
            errors.add("代码输出目录为空");
            return errors;
        }
        File dir = new File(outputDir);
        if (!dir.exists() || !dir.isDirectory()) {
            errors.add("代码输出目录不存在: " + outputDir);
            return errors;
        }

        switch (generationType) {
            case VUE_PROJECT -> {
                // Vue 项目必须包含的关键文件
                checkFileExists(dir, "index.html", "缺少入口文件 index.html（Vite 构建必需，必须位于项目根目录）", errors);
                checkFileExists(dir, "package.json", "缺少 package.json（无法执行 npm install 和 npm run build）", errors);
                checkFileExists(dir, "vite.config.js", "缺少 vite.config.js（Vite 构建配置文件）", errors);
                // 检查 src/main.js 或 src/main.ts
                File srcDir = new File(dir, "src");
                if (!srcDir.exists() || !srcDir.isDirectory()) {
                    errors.add("缺少 src 目录（Vue 项目源码目录）");
                } else {
                    File mainJs = new File(srcDir, "main.js");
                    File mainTs = new File(srcDir, "main.ts");
                    if (!mainJs.exists() && !mainTs.exists()) {
                        errors.add("缺少 src/main.js 或 src/main.ts（Vue 产物入口文件）");
                    }
                }
                // 检查 index.html 是否引用了 src/main.js
                File indexHtml = new File(dir, "index.html");
                if (indexHtml.exists()) {
                    try {
                        String content = FileUtil.readUtf8String(indexHtml);
                        if (!content.contains("src/main")) {
                            errors.add("index.html 未引用 src/main.js（缺少 <script type=\"module\" src=\"/src/main.js\"></script>）");
                        }
                    } catch (Exception e) {
                        log.warn("读取 index.html 失败: {}", e.getMessage());
                    }
                }
                // 检查代码中是否引用了不存在的本地资源文件（如 @/assets/logo.png）
                checkUnresolvedLocalAssets(dir, errors);
            }
            case MULTI_FILE -> {
                // 多文件模式必须包含的文件
                checkFileExists(dir, "index.html", "缺少 index.html 主文档文件", errors);
                checkFileExists(dir, "style.css", "缺少 style.css 样式文件", errors);
                checkFileExists(dir, "script.js", "缺少 script.js 脚本文件", errors);
            }
            case HTML -> {
                // 单 HTML 模式至少需要一个 .html 文件
                File[] htmlFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".html"));
                if (htmlFiles == null || htmlFiles.length == 0) {
                    errors.add("输出目录中未找到任何 .html 文件");
                }
            }
            default -> {
                // 其他类型不做结构性检查
            }
        }

        if (!errors.isEmpty()) {
            log.warn("项目结构检查发现问题: {}", errors);
        }
        return errors;
    }

    /**
     * 检查文件是否存在
     */
    private static void checkFileExists(File dir, String fileName, String errorMessage, List<String> errors) {
        File file = new File(dir, fileName);
        if (!file.exists()) {
            errors.add(errorMessage);
        }
    }

    /**
     * 检查代码中是否引用了不存在的本地资源文件（Vue 项目常见构建失败原因）
     * 例如：import logo from '@/assets/logo.png' 或 <img src="/src/assets/logo.png">
     */
    private static void checkUnresolvedLocalAssets(File projectDir, List<String> errors) {
        File srcDir = new File(projectDir, "src");
        if (!srcDir.exists()) return;

        List<File> sourceFiles = new ArrayList<>();
        collectSourceFiles(srcDir, sourceFiles);

        // 匹配 @/assets/xxx 和 /src/assets/xxx 形式的本地资源引用
        Pattern importPattern = Pattern.compile("['\"](@/assets/[^'\"]+|/src/assets/[^'\"]+)['\"]");
        Set<String> reportedAssets = new HashSet<>();

        for (File sourceFile : sourceFiles) {
            try {
                String content = FileUtil.readUtf8String(sourceFile);
                Matcher matcher = importPattern.matcher(content);
                while (matcher.find()) {
                    String importPath = matcher.group(1);
                    // 统一转换为相对项目根目录的路径
                    String relativePath;
                    if (importPath.startsWith("@/")) {
                        relativePath = "src/" + importPath.substring(2);
                    } else if (importPath.startsWith("/src/")) {
                        relativePath = importPath.substring(1);
                    } else {
                        continue;
                    }

                    File targetFile = new File(projectDir, relativePath);
                    if (!targetFile.exists() && reportedAssets.add(relativePath)) {
                        String sourceRelative = sourceFile.getAbsolutePath()
                                .substring(projectDir.getAbsolutePath().length() + 1)
                                .replace('\\', '/');
                        errors.add("文件 " + sourceRelative + " 引用了不存在的本地资源: " + importPath
                                + "（构建会报 Rollup failed to resolve import 错误）。请改用在线图片 URL 或使用工具创建该文件");
                    }
                }
            } catch (Exception e) {
                log.warn("检查本地资源引用失败: {}", sourceFile.getAbsolutePath());
            }
        }
    }

    /**
     * 递归收集目录下的 .vue / .js / .ts 源码文件
     */
    private static void collectSourceFiles(File dir, List<File> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                String name = file.getName();
                if (!"node_modules".equals(name) && !"dist".equals(name) && !".git".equals(name)) {
                    collectSourceFiles(file, result);
                }
            } else {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".vue") || name.endsWith(".js") || name.endsWith(".ts")) {
                    result.add(file);
                }
            }
        }
    }

    /**
     * 根据生成类型构建结构性修复建议
     */
    private static List<String> buildStructuralSuggestions(CodeGeneratorTypeEnum generationType) {
        return switch (generationType) {
            case VUE_PROJECT -> List.of(
                    "确保项目根目录包含 index.html，内容包括 <script type=\"module\" src=\"/src/main.js\"></script>",
                    "确保 package.json 包含 vue、vue-router 依赖和 vite、@vitejs/plugin-vue 开发依赖",
                    "确保 vite.config.js 配置了 base: './' 和 vue 插件",
                    "确保 src/main.js 创建 Vue 产物并挂载到 #app",
                    "禁止引用未创建的本地资源文件（如 @/assets/logo.png），图片请使用在线 URL"
            );
            case MULTI_FILE -> List.of(
                    "确保 index.html 引用了 style.css 和 script.js",
                    "确保三个文件都存在且内容完整"
            );
            case HTML -> List.of(
                    "确保生成了完整的 HTML 文件"
            );
            default -> List.of();
        };
    }
}