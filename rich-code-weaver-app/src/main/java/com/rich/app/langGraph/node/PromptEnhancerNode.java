package com.rich.app.langGraph.node;

import cn.hutool.core.util.StrUtil;
import com.rich.ai.model.CodeReviewResponse;
import com.rich.app.langGraph.state.WorkflowContext;
import com.rich.model.enums.CodeGeneratorTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 提示词增强节点（强化提示词 或 代码错误修复提示词）
 *
 * @author DuRuiChi
 * @create 2025/9/11
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
        // 1.非第一次生成：已进行过代码质检，并且未通过,则直接构建代码错误修复提示词
        CodeReviewResponse codeReviewResponse = context.getCodeReviewResponse();
        // 已进行过代码质检，并且未通过
        if (isQualityCheckFailed(codeReviewResponse)) {
            // 构建代码修复提示词（区分生成类型，指导局部修复）
            return buildErrorFixPrompt(context);
        }

        // 2.第一次生成：构建强化后的提示词，资源前置 + 强制指令 + 用户需求在后
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
