package com.rich.richcodeweaver.langGraph.node;

import cn.hutool.core.util.StrUtil;
import com.rich.richcodeweaver.langGraph.state.WorkflowContext;
import com.rich.richcodeweaver.model.aiChatResponse.CodeReviewResponse;
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
            // 构建代码修复提示词
            return buildErrorFixPrompt(codeReviewResponse);
        }

        // 2.第一次生成：构建强化后的提示词，用于更好的生成代码
        // 获取图片资源
        String imageListStr = context.getImageListStr();
        // 构建增强后的提示词
        StringBuilder enhancedPromptBuilder = new StringBuilder();
        enhancedPromptBuilder.append(context.getOriginalPrompt());
        // 添加图片信息
        if (StrUtil.isNotBlank(imageListStr)) {
            enhancedPromptBuilder.append("\n\n## 可用素材资源\n");
            enhancedPromptBuilder.append("在生成网站时，推荐使用以下图片资源（不必再调用图片搜索工具和图片生成工具）：\n");
            // 拼接 AI 获取到的图片资源
            enhancedPromptBuilder.append(imageListStr);
        }
        // 补充整理后的网络资源文档
        String webResourceListStr = context.getWebResourceListStr();
        if (StrUtil.isNotBlank(webResourceListStr)) {
            enhancedPromptBuilder.append("\n\n## 可用素材\n");
            enhancedPromptBuilder.append("在生成网站时，可以参考以下素材，或调用联网搜索工具和网页抓取工具作补充：\n");
            // 拼接 AI 获取到的网络资源
            enhancedPromptBuilder.append(webResourceListStr);
        }
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
     * 构造错误修复提示词
     */
    private static String buildErrorFixPrompt(CodeReviewResponse codeReviewResponse) {
        StringBuilder errorInfo = new StringBuilder();
        errorInfo.append("\n\n## 上次生成的代码存在以下问题，请修复：\n");
        // 添加错误列表
        codeReviewResponse.getErrorList().forEach(error ->
                errorInfo.append("- ").append(error).append("\n"));
        // 添加修复建议（如果有）
        if (codeReviewResponse.getSuggestionList() != null && !codeReviewResponse.getSuggestionList().isEmpty()) {
            errorInfo.append("\n## 修复建议：\n");
            codeReviewResponse.getSuggestionList().forEach(suggestion ->
                    errorInfo.append("- ").append(suggestion).append("\n"));
        }
        errorInfo.append("\n请根据上述问题和建议重新生成代码，确保修复所有提到的问题。");
        return errorInfo.toString();
    }

}
