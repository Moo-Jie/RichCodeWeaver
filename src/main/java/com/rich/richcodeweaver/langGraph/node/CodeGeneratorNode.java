package com.rich.richcodeweaver.langGraph.node;

import com.rich.richcodeweaver.langGraph.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 代码生成节点
 *
 * @author DuRuiChi
 * @create 2025/9/11
 **/
@Slf4j
public class CodeGeneratorNode {
    /**
     * 异步创建代码生成节点
     *
     * @return 代码生成节点
     */
    public static AsyncNodeAction<MessagesState<String>> create() {
        // 返回一个异步节点
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("\n 正在执行节点: AI 代码生成。\n");

            // TODO 实现逻辑

            // 更新状态
            // TODO 模拟工作流上下文状态内容的更新
            context.setCurrentStep("AI 代码生成已完成");
            context.setGeneratedCodeDir("/appCode/code_output/genType_AppId");
            log.info("\n AI 代码生成节点运行完成。\n");
            return WorkflowContext.saveContext(context);
        });
    }
}
