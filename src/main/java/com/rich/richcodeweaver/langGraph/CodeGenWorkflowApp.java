package com.rich.richcodeweaver.langGraph;

import cn.hutool.core.util.StrUtil;
import com.rich.richcodeweaver.exception.BusinessException;
import com.rich.richcodeweaver.exception.ErrorCode;
import com.rich.richcodeweaver.langGraph.node.*;
import com.rich.richcodeweaver.langGraph.state.WorkflowContext;
import com.rich.richcodeweaver.model.enums.ChatHistoryTypeEnum;
import com.rich.richcodeweaver.model.enums.CodeGeneratorTypeEnum;
import com.rich.richcodeweaver.service.ChatHistoryService;
import com.rich.richcodeweaver.utils.aiUtils.streamHandle.CommonStreamHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

/**
 * AI 代码生成工作流应用类
 * 负责构建和执行代码生成的完整工作流程，包括资源收集、提示词增强、代码生成和项目构建等步骤
 *
 * @author DuRuiChi
 * @create 2025/9/14
 **/
@Slf4j
public class CodeGenWorkflowApp {
    /**
     * 条件边判断参数映射，用于根据代码生成类型决定工作流路由
     * key: 路由条件判断结果
     * value: 目标节点或结束标识
     **/
    private final Map<String, String> BUILD_ROUTE_MAP = Map.of(
            // 需要构建的情况，指向项目构建节点
            "build", "project_builder",
            // 跳过构建直接结束工作流
            "skip_build", END
    );
    @Resource
    private CommonStreamHandler commonStreamHandler;

    /**
     * 创建完整的工作流
     * 工作流包含以下步骤：搜索图片资源 → 提示词强化 → 代码生成类型规划 → 代码生成 → 项目构建(条件性)
     *
     * @return CompiledGraph<MessagesState < String>> 编译后的工作流实例
     * @throws BusinessException 当工作流创建失败时抛出
     * @author DuRuiChi
     * @create 2025/9/14
     **/
    public CompiledGraph<MessagesState<String>> createWorkflow() {
        try {
            return new MessagesStateGraph<String>()
                    // 添加工作流节点
                    // 图片采集节点：收集与项目相关的图像资源
                    .addNode("image_collector", ImageResourceNode.create())
                    // 提示词增强节点：优化和丰富原始提示词
                    .addNode("prompt_enhancer", PromptEnhancerNode.create())
                    // AI 自主选择代码类型节点：确定最适合的代码生成类型
                    .addNode("ai_code_generator_type_strategy", AiCodeGeneratorTypeStrategyNode.create())
                    // 代码生成节点：根据增强后的提示词生成代码
                    .addNode("code_generator", CodeGeneratorNode.create())
                    // 项目构建节点：构建生成的项目（条件性执行）
                    .addNode("project_builder", ProjectBuilderNode.create())

                    // 添加边，连接节点形成工作流
                    .addEdge(START, "image_collector")  // 起始节点到图片采集
                    .addEdge("image_collector", "prompt_enhancer")  // 图片采集到提示词增强
                    .addEdge("prompt_enhancer", "ai_code_generator_type_strategy")  // 提示词增强到类型策略
                    .addEdge("ai_code_generator_type_strategy", "code_generator")  // 类型策略到代码生成
                    // 条件边：根据代码生成类型决定是否执行项目构建
                    .addConditionalEdges("code_generator",
                            // 异步执行路由判断逻辑
                            edge_async(this::routeBuildOrSkip),
                            // 根据路由结果查询映射，确定下一个节点
                            BUILD_ROUTE_MAP)
                    .addEdge("project_builder", END)  // 项目构建到结束
                    // 编译工作流
                    .compile();
        } catch (GraphStateException e) {
            log.error("工作流创建失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "工作流创建失败");
        }
    }

    /**
     * 执行工作流
     * 通过 ServerSentEvent (SSE) 实时推送工作流执行进度
     *
     * @param originalPrompt     原始提示词，描述需要生成的代码功能
     * @param type               代码生成器的不同模式，影响工作流的执行路径
     * @param appId              应用ID，用于标识不同的生成任务
     * @param chatHistoryService 对话历史服务，用于存储和管理对话记录
     * @param userId             用户ID，用于关联生成任务的用户
     * @return Flux<String> SSE事件流，包含工作流执行过程中的各种事件
     * @author DuRuiChi
     * @create 2025/9/14
     **/
    public Flux<ServerSentEvent<String>> executeWorkflow(String originalPrompt,
                                                         CodeGeneratorTypeEnum type,
                                                         Long appId,
                                                         ChatHistoryService chatHistoryService,
                                                         Long userId) {
        // 构建 Agent 工作流风格的响应流
        Flux<String> fluxStream = Flux.create(sink -> {
            // 使用虚拟线程执行工作流，避免阻塞主线程
            Thread.startVirtualThread(() -> {
                try {
                    // 初始化工作流上下文
                    WorkflowContext initialContext = WorkflowContext.builder()
                            .appId(appId)
                            .originalPrompt(originalPrompt)
                            .generationType(type)
                            .currentStep("工作流初始化")
                            .build();

                    // 发送工作流开始事件 - Agent 风格输出
                    sink.next(" **代码生成 Agent 启动中...**\n" +
                            "**任务概览:**\n" +
                            "   • 应用ID: " + appId + "\n" +
                            "   • 生成类型: " + type.getValue() + "\n" +
                            "   • 原始需求: " + (originalPrompt.length() > 100 ? originalPrompt.substring(0, 100) + "..." : originalPrompt) + "\n" +
                            "   • 用户ID: " + userId + "\n\n" +
                            "**开始执行智能代码生成工作流...**\n");

                    CompiledGraph<MessagesState<String>> workflow = createWorkflow();
                    // 生成可视化工作流图
                    GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
                    log.info("工作流图:\n{}", graph.content());

                    // 发送工作流架构信息
                    sink.next("""
                            **工作流架构已构建完成**
                               • 节点数量: 5个核心处理节点
                               • 流程路径: 图片采集 → 提示词增强 → 类型策略 → 代码生成 → 项目构建
                               • 条件分支: 根据生成类型智能选择构建策略
                            """);

                    // 执行工作流并跟踪进度
                    int stepCounter = 1;
                    String[] stepNames = {"图片资源采集", "提示词智能增强", "代码类型策略分析", "智能代码生成", "项目构建部署"};

                    for (NodeOutput<MessagesState<String>> step : workflow.stream(
                            Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
                        log.info("\n--- 第 {} 步完成 ---\n", stepCounter);
                        // 获取当前步骤的上下文
                        WorkflowContext currentContext = WorkflowContext.getContext(step.state());
                        if (currentContext != null) {
                            // 构建丰富的步骤信息
                            StringBuilder stepInfo = new StringBuilder();

                            // 步骤标题和进度
                            String stepName = stepCounter <= stepNames.length ? stepNames[stepCounter - 1] : currentContext.getCurrentStep();

                            stepInfo.append(String.format("**第%d步: %s** \n", stepCounter, stepName));
                            stepInfo.append(String.format("• 执行状态: %s\n", currentContext.getCurrentStep()));
                            stepInfo.append(String.format("• 应用ID: %d\n", appId));

                            // 展示更多 WorkflowContext 字段信息
                            if (StrUtil.isNotBlank(currentContext.getOriginalPrompt())) {
                                stepInfo.append(String.format("• 原始提示词长度: %d字符\n", currentContext.getOriginalPrompt().length()));
                            }

                            if (currentContext.getCodeGenType() != null) {
                                stepInfo.append(String.format("• 代码生成类型: %s\n", currentContext.getCodeGenType().getValue()));
                            }

                            if (currentContext.getGenerationType() != null) {
                                stepInfo.append(String.format("• 生成策略类型: %s\n", currentContext.getGenerationType().getValue()));
                            }

                            // 图片资源信息
                            if (currentContext.getImageList() != null && !currentContext.getImageList().isEmpty()) {
                                stepInfo.append(String.format("• 收集图片资源: %d张\n", currentContext.getImageList().size()));
                                stepInfo.append("• 图片处理状态: 已完成资源解析和优化\n");
                            }

                            if (StrUtil.isNotBlank(currentContext.getImageListStr())) {
                                stepInfo.append(String.format("• 图片资源字符串长度: %d字符\n", currentContext.getImageListStr().length()));
                            }

                            // 提示词增强信息
                            if (StrUtil.isNotBlank(currentContext.getEnhancedPrompt())) {
                                stepInfo.append("• 提示词增强: 已完成智能优化\n");
                                stepInfo.append(String.format("• 增强后长度: %d字符 (提升了 %.1f%%)\n",
                                        currentContext.getEnhancedPrompt().length(),
                                        ((double) (currentContext.getEnhancedPrompt().length() - currentContext.getOriginalPrompt().length()) / currentContext.getOriginalPrompt().length()) * 100));
                            }

                            // 输出目录信息
                            if (StrUtil.isNotBlank(currentContext.getOutputDir())) {
                                stepInfo.append(String.format("• 代码输出目录: %s\n", currentContext.getOutputDir()));
                                stepInfo.append("• 文件生成状态: 代码文件已成功创建\n");
                            }

                            // 部署目录信息
                            if (StrUtil.isNotBlank(currentContext.getDeployDir())) {
                                stepInfo.append(String.format("• 项目部署目录: %s\n", currentContext.getDeployDir()));
                                stepInfo.append("• 构建状态: 项目构建和部署完成\n");
                            }

                            // 错误信息处理
                            if (StrUtil.isNotBlank(currentContext.getErrorMessage())) {
                                stepInfo.append(String.format("• 异常信息: %s\n", currentContext.getErrorMessage()));
                            }

                            // 进度指示器
                            int totalSteps = 5; // 总步骤数
                            int progress = (stepCounter * 100) / totalSteps;
                            stepInfo.append(String.format("• 整体进度: %d%% [%s%s]\n",
                                    progress,
                                    "█".repeat(progress / 10),
                                    "░".repeat(10 - progress / 10)));

                            stepInfo.append("\n");

                            sink.next(stepInfo.toString());
                            log.info("当前步骤上下文: {}", currentContext);
                        }
                        stepCounter++;
                    }

                    // 发送工作流完成事件 - Agent 风格总结
                    String completionInfo = "**代码生成工作流执行完成!**\n\n" +
                            "**执行统计:**\n" +
                            String.format("   • 应用ID: %d\n", appId) +
                            String.format("   • 总执行步骤: %d个\n", stepCounter - 1) +
                            String.format("   • 用户ID: %d\n", userId) +
                            String.format("   • 生成类型: %s\n", type.getValue()) +
                            "• 执行状态: 全部完成\n\n" +
                            "**Agent 任务完成，代码已准备就绪！**\n";

                    sink.next(completionInfo);
                    log.info("代码生成工作流执行完成！应用ID: {}", appId);
                    sink.complete();
                } catch (Exception e) {
                    log.error("工作流执行失败，应用ID: {}，错误信息: {}", appId, e.getMessage(), e);
                    // 发送错误事件 - Agent 风格错误处理
                    String errorInfo = "**工作流执行异常**\n\n" +
                            "**异常详情:**\n" +
                            String.format("   • 应用ID: %d\n", appId) +
                            String.format("   • 用户ID: %d\n", userId) +
                            String.format("   • 异常类型: %s\n", e.getClass().getSimpleName()) +
                            String.format("   • 错误信息: %s\n", e.getMessage()) +
                            String.format("   • 生成类型: %s\n", type.getValue()) +
                            "\n**建议操作:** 请检查输入参数或联系技术支持\n";

                    sink.next(errorInfo);
                    sink.error(e);
                }
            });
        });
        // 收集 AI 响应内容，保存到对话历史，并进一步处理为响应给前端的最终内容
        StringBuilder aiResponseBuilder = new StringBuilder();
        return commonStreamHandler.handleStream(
                fluxStream
                        // 过滤空字串
                        .filter(StrUtil::isNotEmpty)
                        // 流结束后，保存 AI 响应到对话历史
                        .doOnComplete(() -> {
                            // 保存 AI 响应到对话历史
                            String aiResponse = aiResponseBuilder.toString();
                            if (StrUtil.isNotBlank(aiResponse)) {
                                chatHistoryService.addChatMessage(appId,
                                        aiResponse,
                                        ChatHistoryTypeEnum.AI.getValue(),
                                        userId);
                            }
                        }),
                chatHistoryService, appId, userId, aiResponseBuilder);
    }

    /**
     * 条件边的判断方法，根据代码生成类型判断是否需要构建项目
     * HTML 和 MULTI_FILE 类型不需要构建，直接跳过构建步骤
     *
     * @param state 消息状态，包含工作流上下文信息
     * @return String 路由结果，"skip_build"表示跳过构建，"build"表示需要构建
     * @author DuRuiChi
     * @create 2025/9/14
     **/
    private String routeBuildOrSkip(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        CodeGeneratorTypeEnum generationType = context.getGenerationType();
        // HTML 和 MULTI_FILE 类型不需要构建，直接结束
        if (generationType == CodeGeneratorTypeEnum.HTML || generationType == CodeGeneratorTypeEnum.MULTI_FILE) {
            return "skip_build";
        }
        // 项目工程模式需要构建
        return "build";
    }
}