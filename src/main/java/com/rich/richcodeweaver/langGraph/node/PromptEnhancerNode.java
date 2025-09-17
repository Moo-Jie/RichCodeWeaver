package com.rich.richcodeweaver.langGraph.node;

import cn.hutool.core.util.StrUtil;
import com.rich.richcodeweaver.langGraph.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 提示词增强节点
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
            String enhancedPrompt = enhancedPromptBuilder.toString();
            // 补充整理后的网络资源文档
            String webResourceListStr = context.getWebResourceListStr();
            if (StrUtil.isNotBlank(webResourceListStr)) {
                enhancedPromptBuilder.append("\n\n## 可用素材\n");
                enhancedPromptBuilder.append("在生成网站时，可以参考以下素材，或调用联网搜索工具和网页抓取工具作补充：\n");
                // 拼接 AI 获取到的网络资源
                enhancedPromptBuilder.append(webResourceListStr);
            }
            // 更新状态
            context.setCurrentStep("提示词增强已完成");
            context.setEnhancedPrompt(enhancedPrompt);
            return WorkflowContext.saveContext(context);
        });
    }
}
