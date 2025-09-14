package com.rich.richcodeweaver.langGraph.node;

import com.rich.richcodeweaver.langGraph.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 项目构建节点
 *
 * @author DuRuiChi
 * @create 2025/9/11
 **/
@Slf4j
public class ProjectBuilderNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("\n 正在执行节点: 项目构建。\n");

            // TODO 实现逻辑

            // TODO 模拟工作流上下文状态内容的更新
            context.setCurrentStep("项目构建");
            context.setBuildResultDir("/appCode/code_deploy/deployKey");
            log.info("\n 项目构建节点运行完成。\n");
            return WorkflowContext.saveContext(context);
        });
    }
}
