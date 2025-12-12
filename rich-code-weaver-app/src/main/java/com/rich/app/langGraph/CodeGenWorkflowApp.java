package com.rich.app.langGraph;

import cn.hutool.core.util.StrUtil;
import com.rich.ai.model.CodeReviewResponse;
import com.rich.app.langGraph.node.*;
import com.rich.app.langGraph.state.WorkflowContext;
import com.rich.app.service.ChatHistoryService;
import com.rich.app.utils.streamHandle.CommonStreamHandler;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.model.enums.ChatHistoryTypeEnum;
import com.rich.model.enums.CodeGeneratorTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
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
@Component
public class CodeGenWorkflowApp {
    /**
     * 代码审查最大重试次数
     */
    private static final int MAX_CODE_REVIEW_ATTEMPTS = 2;

    /**
     * 条件边判断参数映射，用于根据代码生成类型决定工作流路由
     * key: 路由条件判断结果
     * value: 目标节点或结束标识
     **/
    private final Map<String, String> ROUTE_RESULTS = Map.of(
            // 代码审查通过，需要构建的情况，指向项目构建节点
            "code_review_succeeded_and_build", "project_builder",
            // 代码审查通过，跳过构建直接结束工作流
            "code_review_succeeded_and_skip_build", END,
            // 代码审查未通过，返回到提示词增强节点
            "code_review_failed", "prompt_enhancer"
    );

    // 收集 AI 响应内容，用于保存到对话历史
    private final StringBuilder aiResponseBuilder = new StringBuilder();

    @Resource
    private CommonStreamHandler commonStreamHandler;

    /**
     * 创建完整的工作流
     * 工作流包含以下步骤：网络资源整理 → 图片资源采集 → 提示词增强 → 代码类型策略 → 代码生成 → AI代码审查 → 项目构建(条件性)
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
                    // 图片采集节点：收集与项目相关的图像资源（单独调用专精于图片收集的 AI 模型）
                    .addNode("web_resource_organizer", WebResourceOrganizeNode.create())
                    // 图片采集节点：收集与项目相关的图像资源（单独调用专精于图片收集的 AI 模型）
                    .addNode("image_collector", ImageResourceNode.create())
                    // 提示词增强节点：优化和丰富原始提示词
                    .addNode("prompt_enhancer", PromptEnhancerNode.create())
                    // AI 自主选择代码类型节点：确定最适合的代码生成类型
                    .addNode("ai_code_generator_type_strategy", AiCodeGeneratorTypeStrategyNode.create())
                    // 代码生成节点：根据增强后的提示词生成代码
                    .addNode("code_generator", CodeGeneratorNode.create())
                    // 代码审查节点：评估生成的代码质量
                    .addNode("ai_code_reviewer", AICodeReviewNode.create())
                    // 项目构建节点：构建生成的项目（条件性执行）
                    .addNode("project_builder", ProjectBuilderNode.create())

                    // 添加边，连接节点形成工作流
                    .addEdge(START, "web_resource_organizer")  // 起始节点到网络资源整理
                    .addEdge("web_resource_organizer", "image_collector")  // 网络资源整理到图片采集
                    .addEdge("image_collector", "prompt_enhancer")  // 图片采集到提示词增强
                    .addEdge("prompt_enhancer", "ai_code_generator_type_strategy")  // 提示词增强到类型策略
                    .addEdge("ai_code_generator_type_strategy", "code_generator")  // 类型策略到代码生成
                    .addEdge("code_generator", "ai_code_reviewer")  // 代码生成到代码审查
                    // 条件边：根据代码生成类型决定是否执行项目构建
                    .addConditionalEdges("ai_code_reviewer",
                            // 异步执行路由判断逻辑
                            edge_async(this::nodeRouter),
                            // 根据路由结果查询映射，确定下一个节点
                            ROUTE_RESULTS)
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

                    // 发送工作流开始事件
                    String startInfo = "\n\n# 代码生成 Agent 启动中...\n\n" +
                            "**应用ID:** " + appId + "\n\n" +
                            "**生成类型:** " + type.getValue() + "\n\n" +
                            "**原始需求:** " + (originalPrompt.length() > 100 ? originalPrompt.substring(0, 100) + "..." : originalPrompt) + "\n\n";
                    sink.next(startInfo);
                    aiResponseBuilder.append(startInfo); // 收集开始信息

                    CompiledGraph<MessagesState<String>> workflow = createWorkflow();
                    // 生成可视化工作流图
                    GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
                    log.info("\n工作流图:\n{}", graph.content());

                    String executionStartInfo = "\n\n## 🎬 开始执行规划节点\n\n";
                    sink.next(executionStartInfo);
                    aiResponseBuilder.append(executionStartInfo); // 收集执行开始信息

                    for (NodeOutput<MessagesState<String>> step : workflow.stream(
                            Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
                        
                        // 获取当前步骤的上下文
                        WorkflowContext currentContext = WorkflowContext.getContext(step.state());
                        if (currentContext != null) {
                            StringBuilder stepInfo = new StringBuilder();
                            String nodeName = step.node();

                            stepInfo.append(String.format("\n\n### ✅ 执行步骤: %s\n\n", currentContext.getCurrentStep()));
                            
                            // 根据节点类型输出特定信息，避免冗余
                            switch (nodeName) {
                                case "web_resource_organizer":
                                    if (StrUtil.isNotBlank(currentContext.getWebResourceListStr())) {
                                        stepInfo.append(String.format("- 整理网络资源: %d 字符\n", currentContext.getWebResourceListStr().length()));
                                    }
                                    break;
                                case "image_collector":
                                    if (currentContext.getImageList() != null && !currentContext.getImageList().isEmpty()) {
                                        stepInfo.append(String.format("- 收集图片资源: %d 张\n", currentContext.getImageList().size()));
                                    }
                                    break;
                                case "prompt_enhancer":
                                    if (StrUtil.isNotBlank(currentContext.getEnhancedPrompt())) {
                                        stepInfo.append(String.format("- 提示词已增强 (长度: %d 字符)\n", currentContext.getEnhancedPrompt().length()));
                                    }
                                    break;
                                case "ai_code_generator_type_strategy":
                                    if (currentContext.getCodeGenType() != null) {
                                        stepInfo.append(String.format("- 确定的代码生成类型: %s\n", currentContext.getCodeGenType().getValue()));
                                    }
                                    break;
                                case "code_generator":
                                    if (StrUtil.isNotBlank(currentContext.getOutputDir())) {
                                        stepInfo.append(String.format("- 代码输出目录: %s\n", currentContext.getOutputDir()));
                                    }
                                    stepInfo.append("\n**等待 AI 代码审查...**\n");
                                    break;
                                case "ai_code_reviewer":
                                    if (currentContext.getCodeReviewResponse() != null) {
                                        boolean passed = currentContext.getCodeReviewResponse().getIsPass();
                                        stepInfo.append(String.format("- 审查结果: %s\n", passed ? "✅ 通过" : "❌ 未通过 (将尝试修复)"));
                                        if (!passed) {
                                             stepInfo.append("- 正在构建修改方案...\n");
                                        }
                                    }
                                    break;
                                case "project_builder":
                                    if (StrUtil.isNotBlank(currentContext.getDeployDir())) {
                                        stepInfo.append(String.format("- 项目部署目录: %s\n", currentContext.getDeployDir()));
                                    }
                                    break;
                            }
                            
                            // 错误信息总是显示
                            if (StrUtil.isNotBlank(currentContext.getErrorMessage())) {
                                stepInfo.append(String.format("\n**⚠️ 异常信息:** %s\n", currentContext.getErrorMessage()));
                            }

                            sink.next(stepInfo.toString());
                            aiResponseBuilder.append(stepInfo);
                            log.info("当前步骤: {} - {}", nodeName, currentContext.getCurrentStep());

                            // 如果是项目构建节点，说明是最后一步，主动结束循环，防止流迭代器阻塞
                            if ("project_builder".equals(nodeName)) {
                                log.info("项目构建完成，主动结束工作流循环");
                                break;
                            }
                        }
                    }

                    // 发送工作流完成事件
                    String completionInfo = "\n\n# 🎉 代码生成任务全部完成！\n\n";
                    sink.next(completionInfo);
                    aiResponseBuilder.append(completionInfo); // 收集完成信息
                    
                    log.info("代码生成工作流执行完成！应用ID: {}", appId);
                    sink.complete();
                } catch (Throwable e) {
                    log.error("工作流执行失败，应用ID: {}，错误信息: {}", appId, e.getMessage(), e);
                    // 发送错误事件
                    String errorInfo = "\n\n# 🛑 Agent 工作流执行异常\n\n" +
                            "**错误信息:** " + e.getMessage() + "\n\n";

                    sink.next(errorInfo);
                    aiResponseBuilder.append(errorInfo); // 收集错误信息
                    sink.error(e);
                }
            });
        });
        // 处理流，把收集 AI 响应内容保存到对话历史，并进一步处理为响应给前端的最终内容
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
     * 条件边的路由逻辑
     *
     * @param state 消息状态，包含工作流上下文信息
     * @return String 自定义的路由结果参数
     * @author DuRuiChi
     * @create 2025/9/14
     **/
    private String nodeRouter(MessagesState<String> state) {
        // 获取工作流上下文
        WorkflowContext context = WorkflowContext.getContext(state);
        CodeReviewResponse codeReviewResponse = context.getCodeReviewResponse();
        CodeGeneratorTypeEnum generationType = context.getGenerationType();

        // 1. 代码审查结果为空，返回失败（异常情况）
        if (codeReviewResponse == null) {
            log.warn("代码审查结果为空，返回失败路由");
            return "code_review_failed";
        }

        Long reviewCount = codeReviewResponse.getReviewCount();
        Boolean isPass = codeReviewResponse.getIsPass();

        log.info("代码审查路由判断 - 审查次数: {}, 是否通过: {}", reviewCount, isPass);

        // 2. 代码审查未通过且审查次数小于最大重试次数，继续重试
        if (!isPass && (reviewCount == null || reviewCount < MAX_CODE_REVIEW_ATTEMPTS)) {
            log.info("代码审查未通过，继续重试 - 当前次数: {}", reviewCount);
            return "code_review_failed";
        }

        // 3. 代码审查通过或已达到最大重试次数，判断是否需要构建项目
        // HTML 和 MULTI_FILE 类型不需要构建
        if (generationType == CodeGeneratorTypeEnum.HTML || generationType == CodeGeneratorTypeEnum.MULTI_FILE) {
            log.info("代码类型 {} 无需构建，直接结束", generationType.getValue());
            return "code_review_succeeded_and_skip_build";
        }

        // 4. 项目工程模式需要构建
        log.info("代码类型 {} 需要构建", generationType.getValue());
        return "code_review_succeeded_and_build";
    }
}