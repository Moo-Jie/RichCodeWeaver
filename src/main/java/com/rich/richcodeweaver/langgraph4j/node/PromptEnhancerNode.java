package com.rich.richcodeweaver.langgraph4j.node;

import com.rich.richcodeweaver.langgraph4j.state.WorkflowContext;
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

            // TODO 实现逻辑
            
            // TODO 模拟工作流上下文状态内容的更新
            String enhancedPrompt = "增强后的提示词:......（省略）";
            context.setCurrentStep("提示词增强已完成");
            context.setEnhancedPrompt(enhancedPrompt);
            log.info("\n 提示词增节点运行完成。\n");
            return WorkflowContext.saveContext(context);
        });
    }
}
