package com.rich.app.langGraph;

import cn.hutool.core.util.StrUtil;
import com.rich.ai.model.CodeReviewResponse;
import com.rich.app.langGraph.node.*;
import com.rich.app.langGraph.state.WorkflowContext;
import com.rich.app.service.ChatHistoryService;
import com.rich.app.utils.streamHandle.CommonStreamHandler;
import com.rich.app.utils.streamHandle.ConversationOutputFormatter;
import com.rich.app.utils.streamHandle.JsonStreamHandler;
import com.rich.ai.aiTools.userProfile.UserProfileGetTool;
import com.rich.common.constant.AiServiceConstant;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
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
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

/**
 * AI 代码生成工作流产物类
 * 负责构建和执行代码生成的完整工作流程，包括资源收集、提示词增强、代码生成和项目构建等步骤
 *
 * @author DuRuiChi
 * @create 2026/1/14
 **/
@Slf4j
@Component
public class CodeGenWorkflowApp {
    /**
     * 质量门禁最大修复轮次
     */
    private static final int MAX_REPAIR_ATTEMPTS = 3;
    /**
     * 条件边判断参数映射，用于根据代码生成类型决定工作流路由
     * key: 路由条件判断结果
     * value: 目标节点或结束标识
     **/
    private final Map<String, String> ROUTE_RESULTS = Map.ofEntries(
            Map.entry("preflight_pass", "ai_code_reviewer"),
            Map.entry("preflight_retry", "prompt_enhancer"),
            Map.entry("preflight_failed", "delivery_gate"),
            Map.entry("review_pass_and_build", "project_builder"),
            Map.entry("review_pass_and_deliver", "delivery_gate"),
            Map.entry("review_retry", "prompt_enhancer"),
            Map.entry("review_failed", "delivery_gate"),
            Map.entry("build_pass", "delivery_gate"),
            Map.entry("build_retry", "prompt_enhancer"),
            Map.entry("build_failed", "delivery_gate")
    );
    @Resource
    private CommonStreamHandler commonStreamHandler;

    @Resource
    private JsonStreamHandler jsonStreamHandler;

    /**
     * 创建完整的工作流
     * 工作流包含以下步骤：资源收集(并行) → 提示词增强 → 代码类型策略 → 代码生成
     * → 结构预检 → AI 审查 → 项目构建(条件性) → 交付门禁
     *
     * @return CompiledGraph<MessagesState < String>> 编译后的工作流实例
     * @throws BusinessException 当工作流创建失败时抛出
     * @author DuRuiChi
     * @create 2026/1/14
     **/
    public CompiledGraph<MessagesState<String>> createWorkflow() {
        try {
            return new MessagesStateGraph<String>()
                    // 添加工作流节点
                    // 资源收集合并节点：并行执行网络资源整理和图片资源收集
                    .addNode("resource_collector", ResourceCollectorNode.create())
                    // 提示词增强节点：优化和丰富原始提示词
                    .addNode("prompt_enhancer", PromptEnhancerNode.create())
                    // AI 自主选择代码类型节点：确定最适合的代码生成类型
                    .addNode("ai_code_generator_type_strategy", AiCodeGeneratorTypeStrategyNode.create())
                    // 代码生成节点：根据增强后的提示词生成代码
                    .addNode("code_generator", CodeGeneratorNode.create())
                    // 项目结构预检节点：执行确定性的文件和资源检查
                    .addNode("project_preflight", ProjectPreflightNode.create())
                    // 代码审查节点：评估生成的代码质量
                    .addNode("ai_code_reviewer", AICodeReviewNode.create())
                    // 项目构建节点：构建生成的项目（条件性执行）
                    .addNode("project_builder", ProjectBuilderNode.create())
                    // 交付门禁节点：只有全部验证通过才允许结束为成功
                    .addNode("delivery_gate", DeliveryGateNode.create())

                    // 添加边，连接节点形成工作流
                    .addEdge(START, "resource_collector")  // 起始节点到资源收集（并行）
                    .addEdge("resource_collector", "prompt_enhancer")  // 资源收集到提示词增强
                    .addEdge("prompt_enhancer", "ai_code_generator_type_strategy")  // 提示词增强到类型策略
                    .addEdge("ai_code_generator_type_strategy", "code_generator")  // 类型策略到代码生成
                    .addEdge("code_generator", "project_preflight")  // 代码生成到结构预检

                    // 条件边：结构预检失败时进入定向修复，耗尽修复轮次后进入失败终态
                    .addConditionalEdges("project_preflight",
                            edge_async(this::preflightRouter),
                            ROUTE_RESULTS)
                    // 条件边：根据 AI 审查结果决定构建、修复或失败
                    .addConditionalEdges("ai_code_reviewer",
                            edge_async(this::reviewRouter),
                            ROUTE_RESULTS)
                    // 条件边：只有真实构建成功才进入交付门禁
                    .addConditionalEdges("project_builder",
                            edge_async(this::buildRouter),
                            ROUTE_RESULTS)
                    .addEdge("delivery_gate", END)
                    // 编译工作流
                    .compile();
        } catch (GraphStateException e) {
            log.error("工作流创建失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "工作流创建失败");
        }
    }

    /**
     * 创建二次修改专用的简化工作流
     * 跳过资源收集和类型策略节点，直接进入：提示词增强 → 代码生成 → 结构预检 → 审查 → 构建 → 交付门禁
     *
     * @return 编译后的修改工作流
     */
    public CompiledGraph<MessagesState<String>> createModificationWorkflow() {
        try {
            return new MessagesStateGraph<String>()
                    .addNode("prompt_enhancer", PromptEnhancerNode.create())
                    .addNode("code_generator", CodeGeneratorNode.create())
                    .addNode("project_preflight", ProjectPreflightNode.create())
                    .addNode("ai_code_reviewer", AICodeReviewNode.create())
                    .addNode("project_builder", ProjectBuilderNode.create())
                    .addNode("delivery_gate", DeliveryGateNode.create())
                    .addEdge(START, "prompt_enhancer")
                    .addEdge("prompt_enhancer", "code_generator")
                    .addEdge("code_generator", "project_preflight")
                    .addConditionalEdges("project_preflight",
                            edge_async(this::preflightRouter),
                            ROUTE_RESULTS)
                    .addConditionalEdges("ai_code_reviewer",
                            edge_async(this::reviewRouter),
                            ROUTE_RESULTS)
                    .addConditionalEdges("project_builder",
                            edge_async(this::buildRouter),
                            ROUTE_RESULTS)
                    .addEdge("delivery_gate", END)
                    .compile();
        } catch (GraphStateException e) {
            log.error("修改工作流创建失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "修改工作流创建失败");
        }
    }

    /**
     * 执行工作流
     * 通过 ServerSentEvent (SSE) 实时推送工作流执行进度
     *
     * @param originalPrompt     原始提示词，描述需要生成的代码功能
     * @param type               代码生成器的不同模式，影响工作流的执行路径
     * @param appId              产物ID，用于标识不同的生成任务
     * @param chatHistoryService 对话历史服务，用于存储和管理对话记录
     * @param userId             用户ID，用于关联生成任务的用户
     * @param isModification     是否为二次修改模式
     * @return Flux<String> SSE事件流，包含工作流执行过程中的各种事件
     * @author DuRuiChi
     * @create 2026/1/14
     **/
    public Flux<ServerSentEvent<String>> executeWorkflow(String originalPrompt,
                                                         CodeGeneratorTypeEnum type,
                                                         Long appId,
                                                         ChatHistoryService chatHistoryService,
                                                         Long userId,
                                                         boolean isModification) {
        UserProfileGetTool.APP_USER_MAPPING.put(appId, userId);
        StringBuilder aiResponseBuilder = new StringBuilder();

        Flux<String> fluxStream = Flux.create(sink -> Thread.startVirtualThread(() -> {
            AtomicBoolean codePhaseStarted = new AtomicBoolean(false);
            AtomicInteger codeGenerationRound = new AtomicInteger(0);
            try {
                WorkflowContext initialContext = WorkflowContext.builder()
                        .appId(appId)
                        .userId(userId)
                        .genMode("workflow")
                        .originalPrompt(originalPrompt)
                        .generationType(type)
                        .currentStep("工作流初始化")
                        .isModification(isModification)
                        .build();

                Runnable startCodePhase = () -> {
                    if (codePhaseStarted.compareAndSet(false, true)) {
                        int round = codeGenerationRound.incrementAndGet();
                        emitStreamBlock(sink, aiResponseBuilder,
                                ConversationOutputFormatter.phaseStarted(stageTitle("code_generator", round), ""));
                    }
                };

                if (type == CodeGeneratorTypeEnum.VUE_PROJECT) {
                    Set<String> seenToolIds = new HashSet<>();
                    CodeGeneratorNode.registerStreamEmitter(appId, chunk -> {
                        startCodePhase.run();
                        String parsed = jsonStreamHandler.parseWorkflowChunk(chunk, seenToolIds);
                        if (StrUtil.isNotEmpty(parsed)) {
                            sink.next(parsed);
                            if (!ConversationOutputFormatter.isTransientMarker(parsed)) {
                                aiResponseBuilder.append(parsed);
                            }
                        }
                    });
                } else {
                    CodeGeneratorNode.registerStreamEmitter(appId, chunk -> startCodePhase.run());
                }

                CompiledGraph<MessagesState<String>> workflow = isModification
                        ? createModificationWorkflow()
                        : createWorkflow();

                String startInfo = "\n\n<!-- WORKFLOW_START -->\n\n"
                        + ConversationOutputFormatter.documentHeader(
                        isModification ? "修改过程" : "生成过程",
                        "系统分步执行 · " + type.getText());
                emitStreamBlock(sink, aiResponseBuilder, startInfo);

                List<String> overviewDetails = List.of(
                        ConversationOutputFormatter.item("代码类型", type.getText()),
                        ConversationOutputFormatter.item("执行方式", "系统分步执行"),
                        ConversationOutputFormatter.item("质量门禁", "结构检查、AI 审查与真实构建")
                );
                emitStreamBlock(sink, aiResponseBuilder,
                        ConversationOutputFormatter.section("执行概览", "已启动", overviewDetails));

                GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
                log.info("\n工作流图:\n{}", graph.content());

                WorkflowContext finalContext = null;
                Map<String, Integer> phaseRounds = new HashMap<>();

                for (NodeOutput<MessagesState<String>> step : workflow.stream(
                        Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
                    WorkflowContext currentContext = WorkflowContext.getContext(step.state());
                    if (currentContext == null) {
                        continue;
                    }

                    finalContext = currentContext;
                    String nodeName = step.node();
                    if (!isDisplayStage(nodeName)) {
                        continue;
                    }
                    boolean codePhaseAlreadyStarted = "code_generator".equals(nodeName)
                            && codePhaseStarted.getAndSet(false);
                    int round;
                    if ("code_generator".equals(nodeName)) {
                        round = codePhaseAlreadyStarted
                                ? codeGenerationRound.get()
                                : codeGenerationRound.incrementAndGet();
                    } else {
                        round = phaseRounds.merge(nodeName, 1, Integer::sum);
                    }

                    String title = stageTitle(nodeName, round);
                    String status = stageStatus(nodeName, currentContext);
                    List<String> details = buildStageDetails(nodeName, currentContext);
                    String stageMarkdown = codePhaseAlreadyStarted
                            ? ConversationOutputFormatter.sectionBody(status, details)
                            : ConversationOutputFormatter.section(title, status, details);
                    emitStreamBlock(sink, aiResponseBuilder, stageMarkdown);
                    log.info("当前步骤: {} - {}", nodeName, currentContext.getCurrentStep());

                    if ("delivery_gate".equals(nodeName)) {
                        break;
                    }
                }

                if (finalContext == null || !finalContext.isDeliveryReady()) {
                    String failureMessage = finalContext != null && StrUtil.isNotBlank(finalContext.getErrorMessage())
                            ? finalContext.getErrorMessage()
                            : "工作流未产生可交付产物";
                    String errorInfo = "\n\n<!-- WORKFLOW_ERROR -->\n\n"
                            + ConversationOutputFormatter.failure(failureMessage);
                    emitStreamBlock(sink, aiResponseBuilder, errorInfo);
                    log.warn("工作流被交付门禁阻断，产物ID: {}，原因: {}", appId, failureMessage);
                    sink.error(new BusinessException(ErrorCode.OPERATION_ERROR, "产物未通过交付质量门禁"));
                    return;
                }

                String completeInfo = "\n\n<!-- WORKFLOW_COMPLETE -->\n\n"
                        + ConversationOutputFormatter.completion("质量门禁已通过，可查看预览、部署或下载");
                emitStreamBlock(sink, aiResponseBuilder, completeInfo);
                log.info("代码生成工作流执行完成，产物ID: {}", appId);
                sink.complete();
            } catch (Throwable e) {
                log.error("工作流执行失败，产物ID: {}，错误信息: {}", appId, e.getMessage(), e);
                emitStreamBlock(sink, aiResponseBuilder,
                        "\n\n<!-- WORKFLOW_ERROR -->\n\n"
                                + ConversationOutputFormatter.failure(
                                StrUtil.blankToDefault(e.getMessage(), "工作流执行异常")));
                sink.error(e);
            } finally {
                CodeGeneratorNode.unregisterStreamEmitter(appId);
            }
        }));

        return commonStreamHandler.handleStream(
                fluxStream.filter(StrUtil::isNotEmpty),
                chatHistoryService,
                appId,
                userId,
                aiResponseBuilder,
                AiServiceConstant.SUMMARY_TYPE_WORKFLOW
        );
    }

    /**
     * 仅展示业务阶段，过滤框架产生的内部节点。
     */
    private boolean isDisplayStage(String nodeName) {
        return switch (nodeName) {
            case "resource_collector", "prompt_enhancer", "ai_code_generator_type_strategy",
                    "code_generator", "project_preflight", "ai_code_reviewer",
                    "project_builder", "delivery_gate" -> true;
            default -> false;
        };
    }

    /**
     * 获取统一阶段标题。
     */
    private String stageTitle(String nodeName, int round) {
        String baseTitle = switch (nodeName) {
            case "resource_collector" -> "资源准备";
            case "prompt_enhancer" -> "需求增强";
            case "ai_code_generator_type_strategy" -> "生成策略";
            case "code_generator" -> "代码生成";
            case "project_preflight" -> "结构检查";
            case "ai_code_reviewer" -> "质量审查";
            case "project_builder" -> "项目构建";
            case "delivery_gate" -> "交付检查";
            default -> "执行阶段";
        };
        return round > 1 ? baseTitle + " · 第 " + round + " 轮" : baseTitle;
    }

    /**
     * 获取统一阶段状态。
     */
    private String stageStatus(String nodeName, WorkflowContext context) {
        return switch (nodeName) {
            case "resource_collector" -> context.getCurrentStep() != null
                    && context.getCurrentStep().contains("跳过") ? "已跳过" : "已完成";
            case "project_preflight", "ai_code_reviewer", "project_builder" -> {
                boolean passed = switch (nodeName) {
                    case "project_preflight" -> context.isPreflightPassed();
                    case "ai_code_reviewer" -> context.getCodeReviewResponse() != null
                            && Boolean.TRUE.equals(context.getCodeReviewResponse().getIsPass());
                    case "project_builder" -> context.isBuildPassed();
                    default -> false;
                };
                yield passed ? "已完成" : "需修复";
            }
            case "delivery_gate" -> context.isDeliveryReady() ? "已完成" : "失败";
            default -> "已完成";
        };
    }

    /**
     * 生成阶段摘要，保持每个节点只输出必要信息。
     */
    private List<String> buildStageDetails(String nodeName, WorkflowContext context) {
        List<String> details = new ArrayList<>();
        switch (nodeName) {
            case "resource_collector" -> {
                details.add(ConversationOutputFormatter.item(
                        "网络资料", resourceSizeSummary(context.getWebResourceListStr())));
                details.add(ConversationOutputFormatter.item(
                        "图片素材", imageResourceSummary(context.getImageListStr())));
                details.add(ConversationOutputFormatter.item("执行方式", "网络资料与图片素材并行处理"));
            }
            case "prompt_enhancer" -> {
                details.add(ConversationOutputFormatter.item("增强结果", "需求、素材与页面结构已合并"));
                details.add(ConversationOutputFormatter.item(
                        "提示词规模", context.getEnhancedPrompt() == null
                                ? "未生成"
                                : ConversationOutputFormatter.count(context.getEnhancedPrompt().length()) + " 字符"));
            }
            case "ai_code_generator_type_strategy" -> {
                String generationType = context.getGenerationType() == null
                        ? "未确定"
                        : context.getGenerationType().getText();
                details.add(ConversationOutputFormatter.item("生成模式", generationType));
                details.add(ConversationOutputFormatter.item("决策方式", "AI 根据需求自动选择"));
            }
            case "code_generator" -> {
                String generationType = context.getGenerationType() == null
                        ? "未确定"
                        : context.getGenerationType().getText();
                details.add(ConversationOutputFormatter.item("代码类型", generationType));
                details.add(ConversationOutputFormatter.item("写入状态", "代码已保存至当前产物工作区"));
            }
            case "project_preflight" -> {
                details.add(ConversationOutputFormatter.item(
                        "结构检查", context.isPreflightPassed() ? "通过" : "未通过"));
                appendLimitedItems(details, "检查问题", context.getPreflightErrors(), 3);
            }
            case "ai_code_reviewer" -> {
                CodeReviewResponse review = context.getCodeReviewResponse();
                boolean passed = review != null && Boolean.TRUE.equals(review.getIsPass());
                details.add(ConversationOutputFormatter.item("审查结论", passed ? "通过" : "未通过"));
                if (review != null) {
                    appendLimitedItems(details, "审查问题", review.getErrorList(), 2);
                    appendLimitedItems(details, "改进建议", review.getSuggestionList(), 2);
                }
            }
            case "project_builder" -> {
                details.add(ConversationOutputFormatter.item("构建结果", context.isBuildPassed() ? "通过" : "未通过"));
                if (!context.isBuildPassed() && StrUtil.isNotBlank(context.getBuildErrorLog())) {
                    details.add(ConversationOutputFormatter.item(
                            "失败摘要", firstLine(context.getBuildErrorLog(), 160)));
                }
            }
            case "delivery_gate" -> {
                details.add(ConversationOutputFormatter.item(
                        "交付结论", context.isDeliveryReady() ? "允许交付" : "阻止交付"));
                if (StrUtil.isNotBlank(context.getErrorMessage())) {
                    details.add(ConversationOutputFormatter.item(
                            "阻断原因", ConversationOutputFormatter.clip(context.getErrorMessage(), 160)));
                }
            }
            default -> details.add(ConversationOutputFormatter.item(
                    "当前状态", StrUtil.blankToDefault(context.getCurrentStep(), "已完成")));
        }
        return details;
    }

    private void appendLimitedItems(List<String> details, String label, List<String> values, int limit) {
        if (values == null || values.isEmpty()) {
            return;
        }
        int count = Math.min(limit, values.size());
        for (int i = 0; i < count; i++) {
            details.add(ConversationOutputFormatter.item(
                    label + " " + (i + 1),
                    ConversationOutputFormatter.clip(values.get(i), 160)));
        }
    }

    private String resourceSizeSummary(String value) {
        if (StrUtil.isBlank(value) || "无".equals(value.trim())) {
            return "无新增";
        }
        return ConversationOutputFormatter.count(value.length()) + " 字符";
    }

    private String imageResourceSummary(String value) {
        if (StrUtil.isBlank(value) || "无".equals(value.trim())) {
            return "无新增";
        }
        long imageCount = value.lines().filter(line -> line.contains("http")).count();
        return imageCount > 0
                ? ConversationOutputFormatter.count(imageCount) + " 项"
                : "已获取";
    }

    private String firstLine(String value, int maxLength) {
        String normalized = value.replace("\r", "\n");
        int lineEnd = normalized.indexOf('\n');
        String line = lineEnd >= 0 ? normalized.substring(0, lineEnd) : normalized;
        return ConversationOutputFormatter.clip(line, maxLength);
    }
    /**
     * 辅助方法：批量发送已标准化的 Markdown 阶段内容
     *
     * @param sink              SSE事件流
     * @param aiResponseBuilder 响应内容构建器
     * @param text              需要输出的文本
     */
    private void emitStreamBlock(FluxSink<String> sink, StringBuilder aiResponseBuilder, String text) {
        sink.next(text);
        aiResponseBuilder.append(text);
    }

    /**
     * 项目结构预检路由
     *
     * @param state 工作流状态
     * @return 路由结果
     */
    private String preflightRouter(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        if (context == null) {
            log.warn("项目结构预检上下文为空，进入失败终态");
            return "preflight_failed";
        }
        if (context.isPreflightPassed()) {
            log.info("项目结构预检通过，进入 AI 代码审查");
            return "preflight_pass";
        }
        if (context.getRepairAttemptCount() < MAX_REPAIR_ATTEMPTS) {
            log.info("项目结构预检未通过，进入第 {} 轮修复", context.getRepairAttemptCount());
            return "preflight_retry";
        }
        log.warn("项目结构预检达到最大修复轮次，阻断交付");
        return "preflight_failed";
    }

    /**
     * AI 代码审查路由
     *
     * @param state 工作流状态
     * @return 路由结果
     */
    private String reviewRouter(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        CodeReviewResponse codeReviewResponse = context == null ? null : context.getCodeReviewResponse();
        boolean reviewPassed = codeReviewResponse != null && Boolean.TRUE.equals(codeReviewResponse.getIsPass());
        if (reviewPassed) {
            if (CodeGeneratorTypeEnum.VUE_PROJECT.equals(context.getGenerationType())) {
                log.info("AI 代码审查通过，进入 Vue 项目真实构建");
                return "review_pass_and_build";
            }
            log.info("AI 代码审查通过，静态页面进入交付门禁");
            return "review_pass_and_deliver";
        }
        if (context != null && context.getRepairAttemptCount() < MAX_REPAIR_ATTEMPTS) {
            log.info("AI 代码审查未通过，进入第 {} 轮修复", context.getRepairAttemptCount());
            return "review_retry";
        }
        log.warn("AI 代码审查达到最大修复轮次，阻断交付");
        return "review_failed";
    }

    /**
     * 项目构建路由
     *
     * @param state 工作流状态
     * @return 路由结果
     */
    private String buildRouter(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        if (context != null && context.isBuildPassed()) {
            log.info("Vue 项目真实构建通过，进入交付门禁");
            return "build_pass";
        }
        if (context != null && context.getRepairAttemptCount() < MAX_REPAIR_ATTEMPTS) {
            log.info("Vue 项目构建未通过，进入第 {} 轮修复", context.getRepairAttemptCount());
            return "build_retry";
        }
        log.warn("Vue 项目构建达到最大修复轮次，阻断交付");
        return "build_failed";
    }
}
