package com.rich.richcodeweaver.langgraph4j.node;

import com.rich.richcodeweaver.langgraph4j.state.WorkflowContext;
import com.rich.richcodeweaver.model.enums.CodeGeneratorTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * AI 自主选择代码类型节点
 *
 * @author DuRuiChi
 * @create 2025/9/11
 **/
@Slf4j
public class AiCodeGeneratorTypeStrategyNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("\n 正在执行节点: AI 自主选择代码类型。\n");

            // TODO 实现逻辑

            // TODO 模拟工作流上下文状态内容的更新
            context.setCurrentStep("智能路由已完成");
            context.setGenerationType(CodeGeneratorTypeEnum.VUE_PROJECT);
            log.info("\n AI 自主选择代码类型节点运行完成。\n");
            return WorkflowContext.saveContext(context);
        });
    }
}
