package com.rich.codeweaver.langGraph.node;

import cn.hutool.core.util.StrUtil;
import com.rich.codeweaver.langGraph.state.WorkflowContext;
import com.rich.codeweaver.model.dto.generator.CodeReviewResponse;
import com.rich.codeweaver.model.enums.CodeGeneratorTypeEnum;
import com.rich.codeweaver.utils.codeConcatenate.CodeConcatenateUtiles;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 提示词增强节点（强化提示词 或 代码错误修复提示词）
 *
 * @author DuRuiChi
 * @create 2026/1/11
 **/
@Slf4j
public class PromptEnhancerNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("\n 正在执行节点: 提示词增强。\n");

            // 更新状态
            context.setCurrentStep("提示词增强已完成");
            // 构建增强后的提示词（强化提示词 或 代码错误修复提示词）
            context.setEnhancedPrompt(buildEnhancedUserPrompt(context));
            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * 构建增强后的用户提示词
     */
    private static String buildEnhancedUserPrompt(WorkflowContext context) {
        // 1.代码质检未通过：构建代码错误修复提示词
        CodeReviewResponse codeReviewResponse = context.getCodeReviewResponse();
        if (isQualityCheckFailed(codeReviewResponse)) {
            return buildErrorFixPrompt(context);
        }

        // 2.二次修改模式：用户对已生成的代码提出修改需求
        if (context.isModification()) {
            return buildModificationPrompt(context);
        }

        // 3.首次生成：构建强化后的提示词，资源前置 + 强制指令 + 用户需求在后
        StringBuilder enhancedPromptBuilder = new StringBuilder();

        // === 素材区域：放在用户需求之前，确保 AI 优先看到并使用 ===
        boolean hasResources = false;
        String imageListStr = context.getImageListStr();
        String webResourceListStr = context.getWebResourceListStr();

        boolean hasImages = StrUtil.isNotBlank(imageListStr) && !"无".equals(imageListStr.trim());
        boolean hasWebResources = StrUtil.isNotBlank(webResourceListStr) && !"无".equals(webResourceListStr.trim());

        if (hasImages || hasWebResources) {
            hasResources = true;
            enhancedPromptBuilder.append("## ⚠️ 必须使用的素材资源（强制要求）\n\n");
            enhancedPromptBuilder.append("以下素材是本系统前置节点专门为本项目搜集和生成的。");
            enhancedPromptBuilder.append("你**必须**在生成的网站中使用这些素材，**禁止忽略**。\n\n");
        }

        // 图片资源（必须使用）
        if (hasImages) {
            enhancedPromptBuilder.append("### 图片素材（必须全部使用，禁止用占位图替代）\n\n");
            enhancedPromptBuilder.append("以下每个图片 URL 你都**必须**用在网站代码中。");
            enhancedPromptBuilder.append("使用方式：`<img src=\"图片URL\">` 或 `:src=\"'图片URL'\"` 或 `background-image: url('图片URL')`。\n");
            enhancedPromptBuilder.append("**禁止**使用 picsum.photos 或任何随机图片网址替代下面的真实图片。");
            enhancedPromptBuilder.append("**禁止**再调用图片搜索工具和图片生成工具。\n\n");
            enhancedPromptBuilder.append(imageListStr);
            enhancedPromptBuilder.append("\n\n");
        }

        // 网络资源（必须采纳关键信息）
        if (hasWebResources) {
            enhancedPromptBuilder.append("### 参考内容（必须将关键信息融入网站）\n\n");
            enhancedPromptBuilder.append("以下是与项目需求相关的网络搜索结果和整理后的素材。");
            enhancedPromptBuilder.append("你必须将其中的**关键文本内容、数据、描述**直接用于网站的对应模块，");
            enhancedPromptBuilder.append("使网站呈现真实、专业的内容，而非占位文字。\n\n");
            enhancedPromptBuilder.append(webResourceListStr);
            enhancedPromptBuilder.append("\n\n");
        }

        if (hasResources) {
            enhancedPromptBuilder.append("⚠️ **再次提醒**：以上提供的每个图片 URL 都必须出现在最终生成的代码中，禁止遗漏任何一张图片。\n\n");
            enhancedPromptBuilder.append("---\n\n");
        }

        // === 用户原始需求 ===
        enhancedPromptBuilder.append("## 用户需求\n\n");
        enhancedPromptBuilder.append(context.getOriginalPrompt());

        return enhancedPromptBuilder.toString();
    }

    /**
     * 进行过代码质检，并且未通过
     */
    private static boolean isQualityCheckFailed(CodeReviewResponse codeReviewResponse) {
        return codeReviewResponse != null &&
                !codeReviewResponse.getIsPass() &&
                codeReviewResponse.getErrorList() != null &&
                !codeReviewResponse.getErrorList().isEmpty();
    }

    /**
     * 构造二次修改提示词（用户对已生成的代码提出修改需求）
     * 对 HTML/MULTI_FILE 模式：将现有代码作为上下文附加，指导精确修改
     * 对 VUE_PROJECT 模式：强调使用工具读取和局部修改
     */
    private static String buildModificationPrompt(WorkflowContext context) {
        CodeGeneratorTypeEnum generationType = context.getGenerationType();
        StringBuilder modPrompt = new StringBuilder();

        modPrompt.append("## 修改需求\n\n");
        modPrompt.append(context.getOriginalPrompt());
        modPrompt.append("\n\n");

        if (generationType == CodeGeneratorTypeEnum.VUE_PROJECT) {
            // VUE_PROJECT 模式：依赖工具读取文件，无需附加代码
            modPrompt.append("### 修改指引\n\n");
            modPrompt.append("请严格按照系统提示词中的「修改流程」执行：\n");
            modPrompt.append("1. 先使用【readDir】了解项目结构\n");
            modPrompt.append("2. 使用【readFile】查看需修改的目标文件\n");
            modPrompt.append("3. 使用【modifyFile】进行精确的局部修改\n");
            modPrompt.append("4. 禁止全量重新生成文件\n");
        } else {
            // HTML / MULTI_FILE 模式：附加现有代码作为上下文
            String outputDir = context.getOutputDir();
            if (StrUtil.isNotBlank(outputDir)) {
                String existingCode = CodeConcatenateUtiles.readAndConcatenateCodeFiles(outputDir);
                if (StrUtil.isNotBlank(existingCode)) {
                    modPrompt.append("### 当前已有代码（请在此基础上修改）\n\n");
                    modPrompt.append(existingCode);
                    modPrompt.append("\n\n");
                }
            }
            modPrompt.append("### 修改指引\n\n");
            modPrompt.append("- 仔细阅读上方的已有代码，仅修改与需求相关的部分\n");
            modPrompt.append("- 保持其他代码不变，确保页面稳定性和风格一致\n");
            modPrompt.append("- 输出修改后的完整代码（格式与之前一致），不要有任何省略\n");
        }

        return modPrompt.toString();
    }

    /**
     * 构造错误修复提示词（区分生成类型，指导局部修复而非全量重新生成）
     */
    private static String buildErrorFixPrompt(WorkflowContext context) {
        CodeReviewResponse codeReviewResponse = context.getCodeReviewResponse();
        CodeGeneratorTypeEnum generationType = context.getGenerationType();
        StringBuilder fixPrompt = new StringBuilder();

        fixPrompt.append("## 代码审查未通过，请按要求修复\n\n");

        // 添加错误列表
        fixPrompt.append("### 发现的问题\n\n");
        codeReviewResponse.getErrorList().forEach(error ->
                fixPrompt.append("- ").append(error).append("\n"));

        // 添加修复建议（如果有）
        if (codeReviewResponse.getSuggestionList() != null && !codeReviewResponse.getSuggestionList().isEmpty()) {
            fixPrompt.append("\n### 修复建议\n\n");
            codeReviewResponse.getSuggestionList().forEach(suggestion ->
                    fixPrompt.append("- ").append(suggestion).append("\n"));
        }

        // 根据生成类型给出不同的修复指令
        fixPrompt.append("\n### 修复要求（必须遵守）\n\n");

        if (generationType == CodeGeneratorTypeEnum.VUE_PROJECT) {
            // Vue 项目模式：使用工具进行局部修改
            fixPrompt.append("你必须按以下步骤进行**局部修复**，禁止重新生成整个项目：\n\n");
            fixPrompt.append("1. 使用【目录读取工具 readDir】查看当前项目结构\n");
            fixPrompt.append("2. 使用【文件读取工具 readFile】查看出问题的文件内容\n");
            fixPrompt.append("3. 使用【文件修改工具 modifyFile】对问题文件进行**局部修改**\n");
            fixPrompt.append("4. 如果缺少关键文件（如 index.html），使用【文件写入工具 creatAndWrite】补充创建\n");
            fixPrompt.append("5. **禁止**使用 creatAndWrite 覆盖所有文件重新生成\n");
            fixPrompt.append("6. 只修复上述问题，不要改动其他正常运行的代码\n");
            fixPrompt.append("7. 修复后确保项目能通过 `npm run build` 成功构建\n");
        } else {
            // HTML / MULTI_FILE 模式：输出完整文件但仅修改问题部分
            fixPrompt.append("请仅针对上述问题进行修复，尽量保持其他代码不变。\n");
            fixPrompt.append("输出修复后的完整代码（格式与之前一致），确保修复所有提到的问题。\n");
        }

        return fixPrompt.toString();
    }

}
