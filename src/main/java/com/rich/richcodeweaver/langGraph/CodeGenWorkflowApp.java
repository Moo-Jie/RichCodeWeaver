package com.rich.richcodeweaver.langGraph;

import cn.hutool.core.util.StrUtil;
import com.rich.richcodeweaver.exception.BusinessException;
import com.rich.richcodeweaver.exception.ErrorCode;
import com.rich.richcodeweaver.langGraph.node.*;
import com.rich.richcodeweaver.langGraph.node.WebResourceOrganizeNode;
import com.rich.richcodeweaver.langGraph.state.WorkflowContext;
import com.rich.richcodeweaver.model.aiChatResponse.CodeReviewResponse;
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
     * 工作流节点名称数组，用于动态获取节点数量和步骤名称
     */
    private static final String[] WORKFLOW_STEP_NAMES = {
            "网络资源整理", "图片资源采集", "提示词智能增强",
            "代码类型策略分析", "智能代码生成", "AI代码审查", "项目构建部署"
    };

    /**
     * 基础工作流步骤数（不包含可能的重复执行）
     */
    private static final int BASE_WORKFLOW_STEPS = WORKFLOW_STEP_NAMES.length;

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

                    // 发送工作流开始事件 - Agent 风格输出
                    String startInfo = "\n\n# 代码生成 Agent 启动中...\n\n" +
                            "\n\n## 📋 一、任务概览\n\n" +
                            "\n**应用ID:** " + appId + "\n\n" +
                            "**生成类型:** " + type.getValue() + "\n\n" +
                            "**原始需求:** " + (originalPrompt.length() > 100 ? originalPrompt.substring(0, 100) + "..." : originalPrompt) + "\n\n" +
                            "**用户ID:** " + userId + "\n\n" +
                            "\n\n## 🔄 二、下面我将开始执行 Agent 智能代码生成工作流，正在初始化工作流...\n\n";
                    sink.next(startInfo);
                    aiResponseBuilder.append(startInfo); // 收集开始信息

                    CompiledGraph<MessagesState<String>> workflow = createWorkflow();
                    // 生成可视化工作流图
                    GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
                    log.info("\n工作流图:\n{}", graph.content());

                    // 发送工作流架构信息
                    String architectureInfo = String.format("""
                            ## 🏗️ 三、本次工作流架构已构建完成
                            
                            **节点数量:** 将采用 %d 个核心处理节点
                            
                            **流程路径:** 网络资源整理 → 图片采集 → 提示词增强 → 类型策略 → 代码生成 → AI代码审查 → 项目构建
                            
                            **条件分支:** 根据代码审查结果和生成类型智能选择构建策略
                            """, BASE_WORKFLOW_STEPS);
                    sink.next(architectureInfo);
                    aiResponseBuilder.append(architectureInfo); // 收集架构信息

                    // 执行工作流并跟踪进度
                    int stepCounter = 1;
                    int totalExpectedSteps = BASE_WORKFLOW_STEPS; // 初始预期步骤数
                    int codeReviewAttempts = 0; // 代码审查尝试次数
                    int actualExecutedSteps = 0; // 实际执行的步骤数（用于显示）

                    String executionStartInfo = "\n\n## 🎬 四、开始执行规划节点\n\n**🤔 正在继续思考...**\n\n";
                    sink.next(executionStartInfo);
                    aiResponseBuilder.append(executionStartInfo); // 收集执行开始信息

                    for (NodeOutput<MessagesState<String>> step : workflow.stream(
                            Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
                        actualExecutedSteps++;
                        log.info(" --- 第 {} 步完成 ---", actualExecutedSteps);
                        // 获取当前步骤的上下文
                        WorkflowContext currentContext = WorkflowContext.getContext(step.state());
                        if (currentContext != null) {
                            // 构建丰富的步骤信息
                            StringBuilder stepInfo = new StringBuilder();

                            // 动态确定步骤名称和总步骤数
                            String stepName = getStepName(currentContext, stepCounter, codeReviewAttempts);

                            // 动态计算总预期步骤数
                            totalExpectedSteps = calculateTotalExpectedSteps(currentContext, type);

                            stepInfo.append(String.format("\n\n## ✅ 第%d步开始执行: %s\n\n", actualExecutedSteps, stepName));
                            stepInfo.append(String.format("\n**🔄 当前状态** 【%s】\n\n", currentContext.getCurrentStep()));
                            stepInfo.append(String.format("**应用ID:** %d\n\n", appId));

                            // 展示更多 WorkflowContext 字段信息
                            if (StrUtil.isNotBlank(currentContext.getOriginalPrompt())) {
                                stepInfo.append(String.format("**原始提示词长度:** %d字符\n\n", currentContext.getOriginalPrompt().length()));
                            }

                            if (currentContext.getCodeGenType() != null) {
                                stepInfo.append(String.format("**代码生成类型:** %s\n\n", currentContext.getCodeGenType().getValue()));
                            }

                            if (currentContext.getGenerationType() != null) {
                                stepInfo.append(String.format("**生成策略类型:** %s\n\n", currentContext.getGenerationType().getValue()));
                            }

                            // 图片资源信息
                            if (currentContext.getImageList() != null && !currentContext.getImageList().isEmpty()) {
                                stepInfo.append(String.format("**收集图片资源:** %d张\n\n", currentContext.getImageList().size()));
                                stepInfo.append("**图片处理状态:** 已完成资源解析和优化\n\n");
                            }

                            // 网络资源信息
                            if (StrUtil.isNotBlank(currentContext.getWebResourceListStr())) {
                                stepInfo.append(String.format("**搜索到了 %d 字符的网络资源** %d\n\n", currentContext.getWebResourceListStr().length(), currentContext.getWebResourceListStr().length()));
                            }

                            // 提示词增强信息
                            if (StrUtil.isNotBlank(currentContext.getEnhancedPrompt())) {
                                stepInfo.append("**提示词增强:** 已完成智能优化\n\n");
                                stepInfo.append(String.format("**增强后长度:** %d字符 (提升了 %.1f%%)\n\n",
                                        currentContext.getEnhancedPrompt().length(),
                                        ((double) (currentContext.getEnhancedPrompt().length() - currentContext.getOriginalPrompt().length()) / currentContext.getOriginalPrompt().length()) * 100));
                            }

                            // 输出目录信息
                            if (StrUtil.isNotBlank(currentContext.getOutputDir())) {
                                stepInfo.append(String.format("**代码输出目录:** %s\n\n", currentContext.getOutputDir()));
                                stepInfo.append("**文件生成状态:** 代码文件已成功创建\n\n");
                            }

                            // 代码审查信息
                            if (currentContext.getCodeReviewResponse() != null) {
                                CodeReviewResponse reviewResponse = currentContext.getCodeReviewResponse();
                                stepInfo.append(String.format("**🔍 代码审查状态:** %s\n\n",
                                        reviewResponse.getIsPass() ? "✅ 审查通过" : "❌ 审查未通过"));
                            }

                            // 部署目录信息
                            if (StrUtil.isNotBlank(currentContext.getDeployDir())) {
                                stepInfo.append(String.format("**项目部署目录:** %s\n\n", currentContext.getDeployDir()));
                                stepInfo.append("**构建状态:** 项目构建和部署完成\n\n");
                            }

                            // 错误信息处理
                            if (StrUtil.isNotBlank(currentContext.getErrorMessage())) {
                                stepInfo.append(String.format("**⚠️ 异常信息:** %s\n\n", currentContext.getErrorMessage()));
                            }

                            // 智能进度指示器 - 处理代码审查重试情况
                            int displayStepNumber;
                            int progress;

                            // 添加代码审查次数信息
                            String reviewInfo = "";
                            if (currentContext.getCodeReviewResponse() != null) {
                                Long reviewCount = currentContext.getCodeReviewResponse().getReviewCount();
                                if (reviewCount != null && reviewCount > 0) {
                                    codeReviewAttempts = reviewCount.intValue();
                                    reviewInfo = String.format(" (代码审查第%d次)", reviewCount);
                                }
                            }

                            // 如果是代码审查重试阶段，显示步数不超过总步数
                            if (codeReviewAttempts > 1 && actualExecutedSteps > totalExpectedSteps) {
                                // 重试时，显示为最后几个步骤的重复执行
                                displayStepNumber = totalExpectedSteps;
                                progress = 100; // 重试时进度保持100%
                            } else {
                                displayStepNumber = Math.min(actualExecutedSteps, totalExpectedSteps);
                                progress = Math.min((displayStepNumber * 100) / totalExpectedSteps, 100);
                            }

                            int filledBars = Math.min(progress / 10, 10); // 限制填充条数不超过10
                            int emptyBars = Math.max(10 - filledBars, 0); // 确保空白条数不为负

                            stepInfo.append(String.format("\n**📊 整体进度:** %d%% [%s%s] (%d/%d步)%s\n\n",
                                    progress,
                                    "█".repeat(filledBars),
                                    "░".repeat(emptyBars),
                                    displayStepNumber,
                                    totalExpectedSteps,
                                    reviewInfo));

                            stepInfo.append("\n\n**🤔 正在继续思考...**\n\n");

                            // 代码生成完成后，添加审查提示
                            if (currentContext.getCurrentStep().equals("代码生成已完成")) {
                                stepInfo.append("\n\n### **接下来开始AI代码审查，评估代码质量...**\n\n");
                            }

                            // 代码审查完成后，根据结果添加相应提示
                            if (currentContext.getCurrentStep().equals("代码审查已完成")) {
                                if (currentContext.getCodeReviewResponse() != null && currentContext.getCodeReviewResponse().getIsPass()) {
                                    stepInfo.append("\n\n### **代码审查通过！接下来开始项目构建部署...**\n\n");
                                } else {
                                    stepInfo.append("\n\n### **代码审查未通过，正在构建修改方案提示词...**\n\n");
                                }
                            }

                            sink.next(stepInfo.toString());
                            // 收集 AI 响应内容，用于保存到历史信息
                            aiResponseBuilder.append(stepInfo);
                            log.info("当前步骤上下文: {}", currentContext);
                        }

                        // 根据当前步骤更新stepCounter
                        if (currentContext != null) {
                            // 如果是重新执行的步骤（代码审查失败后），不增加stepCounter
                            String currentStepName = currentContext.getCurrentStep();
                            if (!currentStepName.contains("已完成") || actualExecutedSteps <= BASE_WORKFLOW_STEPS) {
                                stepCounter++;
                            }
                        } else {
                            stepCounter++;
                        }
                    }

                    // 发送工作流完成事件 - Agent 风格总结
                    String completionInfo = "# 代码生成工作流执行完成!\n\n" +
                            "\n## 📈 执行统计\n\n" +
                            String.format("**应用ID:** %d\n\n", appId) +
                            String.format("**总执行步骤:** %d个\n\n", actualExecutedSteps) +
                            String.format("**代码审查次数:** %d次\n\n", codeReviewAttempts) +
                            String.format("**用户ID:** %d\n\n", userId) +
                            String.format("**生成类型:** %s\n\n", type.getValue()) +
                            "**✅ 当前状态** 【全部完成】\n\n" +
                            "\n\n# Agent 任务完成，代码已准备就绪！\n\n";

                    sink.next(completionInfo);
                    aiResponseBuilder.append(completionInfo); // 收集完成信息

                    String finalInfo = "\n\n# 代码生成工作流执行完成!\n\n";
                    sink.next(finalInfo);
                    aiResponseBuilder.append(finalInfo); // 收集最终信息
                    log.info("代码生成工作流执行完成！应用ID: {}", appId);
                    sink.complete();
                } catch (Exception e) {
                    log.error("工作流执行失败，应用ID: {}，错误信息: {}", appId, e.getMessage(), e);
                    // 发送错误事件 - Agent 风格错误处理
                    String errorInfo = "# Agent 工作流执行异常\n\n" +
                            "## 🔍 异常详情\n\n" +
                            String.format("**应用ID:** %d\n\n", appId) +
                            String.format("**用户ID:** %d\n\n", userId) +
                            String.format("**异常类型:** %s\n\n", e.getClass().getSimpleName()) +
                            String.format("**错误信息:** %s\n\n", e.getMessage()) +
                            String.format("**生成类型:** %s\n\n", type.getValue()) +
                            "\n\n# 🛑 代码生成任务终止，请联系管理员\n\n";

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

    /**
     * 根据当前上下文动态获取步骤名称
     */
    private String getStepName(WorkflowContext context, int stepCounter, int codeReviewAttempts) {
        String currentStep = context.getCurrentStep();

        // 如果是代码审查步骤且有重试，显示重试次数
        if ("代码审查".equals(currentStep) && context.getCodeReviewResponse() != null) {
            Long reviewCount = context.getCodeReviewResponse().getReviewCount();
            if (reviewCount != null && reviewCount > 1) {
                return String.format("AI代码审查 (第%d次重试)", reviewCount);
            }
        }

        // 如果是提示词增强步骤且是因为代码审查失败触发的，显示修复标识
        if ("提示词增强".equals(currentStep) && context.getCodeReviewResponse() != null &&
                !context.getCodeReviewResponse().getIsPass()) {
            return "提示词智能增强 (代码修复)";
        }

        // 如果是代码生成步骤且是重试，显示修复标识
        if ("代码生成".equals(currentStep) && codeReviewAttempts > 1) {
            return "智能代码生成 (代码修复)";
        }

        // 如果是项目构建完成的重复步骤，显示为"项目构建已完成"
        if (currentStep.contains("项目构建已完成")) {
            return "项目构建已完成";
        }

        // 使用预定义的步骤名称或当前步骤名称
        if (stepCounter <= WORKFLOW_STEP_NAMES.length) {
            return WORKFLOW_STEP_NAMES[stepCounter - 1];
        }

        return currentStep;
    }

    /**
     * 动态计算总预期步骤数
     */
    private int calculateTotalExpectedSteps(WorkflowContext context, CodeGeneratorTypeEnum type) {
        int totalSteps = BASE_WORKFLOW_STEPS;

        // 如果是HTML或MULTI_FILE类型，不需要项目构建步骤
        if (type == CodeGeneratorTypeEnum.HTML || type == CodeGeneratorTypeEnum.MULTI_FILE) {
            totalSteps = BASE_WORKFLOW_STEPS - 1; // 减去项目构建步骤
        }

        // 注意：不再动态增加步骤数，因为重试时我们希望保持原有的步骤数显示
        // 这样可以避免出现 8/7步 这样的显示问题
        // 代码审查重试时，进度条会保持在合理范围内

        return totalSteps;
    }
}