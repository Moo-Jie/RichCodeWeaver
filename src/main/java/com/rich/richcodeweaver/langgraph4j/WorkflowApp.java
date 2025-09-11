package com.rich.richcodeweaver.langgraph4j;

import com.rich.richcodeweaver.langgraph4j.node.*;
import com.rich.richcodeweaver.langgraph4j.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

/**
 * 工作流应用程序（ Demo ）
 *
 * @author DuRuiChi
 * @create 2025/8/4
 **/
@Slf4j
public class WorkflowApp {

    public static void main(String[] args) throws GraphStateException {
        // 创建工作流图，存储了工作流的节点和边
        // MessagesState：消息状态，用于存储工作流的状态信息
        CompiledGraph<MessagesState<String>> workflow = new MessagesStateGraph<String>()
                // 添加节点
                // 图片采集节点
                .addNode("image_collector", ImageCollectorNode.create())
                // 提示词增强节点
                .addNode("prompt_enhancer", PromptEnhancerNode.create())
                // AI 自主选择代码类型节点
                .addNode("ai_code_generator_type_strategy", AiCodeGeneratorTypeStrategyNode.create())
                // 代码生成节点
                .addNode("code_generator", CodeGeneratorNode.create())
                // 项目构建节点
                .addNode("project_builder", ProjectBuilderNode.create())
                // 添加边,链接节点
                .addEdge(START, "image_collector")
                .addEdge("image_collector", "prompt_enhancer")
                .addEdge("prompt_enhancer", "ai_code_generator_type_strategy")
                .addEdge("ai_code_generator_type_strategy", "code_generator")
                .addEdge("code_generator", "project_builder")
                .addEdge("project_builder", END)
                // 编译工作流
                .compile();

        // 初始化工作流上下文，用于存储工作流的状态信息
        WorkflowContext initialContext = WorkflowContext.builder()
                // 初始提示词
                .originalPrompt("创建一个鱼皮的个人博客网站")
                // 当前状态
                .currentStep("初始化")
                .build();

        log.info("初始输入: {}", initialContext.getOriginalPrompt());
        log.info("开始执行工作流");

        // 创建一个可视化工作流图
        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("工作流图:\n{}", graph.content());

        // 执行工作流，测试节点执行情况
        int stepCounter = 1;
        // workflow.stream() 启动工作流
        // nodeOutput：节点输出，包含节点执行后的状态信息列表
        // 遍历节点输出，获取每个节点执行完毕后的信息
        for (NodeOutput<MessagesState<String>> nodeOutput : workflow.stream(Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
            log.info("--- 第 {} 步完成 ---", stepCounter);
            // 取出当前节点执行后的自定义上下文状态，包含了所有节点执行完毕后的信息
            WorkflowContext currentContext = WorkflowContext.getContext(nodeOutput.state());
            if (currentContext != null) {
                log.info("当前步骤上下文: {}", currentContext);
            }
            stepCounter++;
        }
        log.info("工作流执行完成！");
    }
}
