package com.rich.app.langGraph;

import cn.hutool.core.util.StrUtil;
import com.rich.ai.model.CodeReviewResponse;
import com.rich.app.langGraph.node.*;
import com.rich.app.langGraph.state.WorkflowContext;
import com.rich.app.service.ChatHistoryService;
import com.rich.app.utils.streamHandle.CommonStreamHandler;
import com.rich.app.utils.streamHandle.JsonStreamHandler;
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    // 流式输出每个字符的延迟（毫秒），模拟 AI 打字效果
    private static final int STREAM_CHAR_DELAY_MS = 15;

    @Resource
    private CommonStreamHandler commonStreamHandler;

    @Resource
    private JsonStreamHandler jsonStreamHandler;

    /**
     * 创建完整的工作流
     * 工作流包含以下步骤：资源收集(并行) → 提示词增强 → 代码类型策略 → 代码生成 → AI代码审查 → 项目构建(条件性)
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
                    // 代码审查节点：评估生成的代码质量
                    .addNode("ai_code_reviewer", AICodeReviewNode.create())
                    // 项目构建节点：构建生成的项目（条件性执行）
                    .addNode("project_builder", ProjectBuilderNode.create())

                    // 添加边，连接节点形成工作流
                    .addEdge(START, "resource_collector")  // 起始节点到资源收集（并行）
                    .addEdge("resource_collector", "prompt_enhancer")  // 资源收集到提示词增强
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
     * 创建二次修改专用的简化工作流
     * 跳过资源收集和类型策略节点，直接进入：提示词增强 → 代码生成 → 代码审查 → 项目构建(条件性)
     */
    public CompiledGraph<MessagesState<String>> createModificationWorkflow() {
        try {
            return new MessagesStateGraph<String>()
                    .addNode("prompt_enhancer", PromptEnhancerNode.create())
                    .addNode("code_generator", CodeGeneratorNode.create())
                    .addNode("ai_code_reviewer", AICodeReviewNode.create())
                    .addNode("project_builder", ProjectBuilderNode.create())
                    .addEdge(START, "prompt_enhancer")
                    .addEdge("prompt_enhancer", "code_generator")
                    .addEdge("code_generator", "ai_code_reviewer")
                    .addConditionalEdges("ai_code_reviewer",
                            edge_async(this::nodeRouter),
                            ROUTE_RESULTS)
                    .addEdge("project_builder", END)
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
        // 每次执行使用局部 StringBuilder，避免并发问题
        StringBuilder aiResponseBuilder = new StringBuilder();
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
                            .isModification(isModification)
                            .build();

                    // 注册流式输出发射器，使代码生成节点能将 AI 流式内容实时转发到前端
                    if (type == CodeGeneratorTypeEnum.VUE_PROJECT) {
                        Set<String> seenToolIds = new HashSet<>();
                        CodeGeneratorNode.registerStreamEmitter(appId, chunk -> {
                            String parsed = jsonStreamHandler.parseJsonChunk(chunk, seenToolIds);
                            if (StrUtil.isNotEmpty(parsed)) {
                                sink.next(parsed);
                                aiResponseBuilder.append(parsed);
                            }
                        });
                    } else {
                        CodeGeneratorNode.registerStreamEmitter(appId, chunk -> {
                            sink.next(chunk);
                            aiResponseBuilder.append(chunk);
                        });
                    }

                    // 根据是否为二次修改选择不同的工作流
                    CompiledGraph<MessagesState<String>> workflow;
                    if (isModification) {
                        workflow = createModificationWorkflow();
                        emitStreamBlock(sink, aiResponseBuilder, "\n\n<!-- WORKFLOW_START -->\n\n" +
                                "# \uD83D\uDD04 代码修改 Agent 启动\n\n" +
                                "---\n\n");
                    } else {
                        workflow = createWorkflow();
                        // 发送工作流开始事件 - 使用结构化格式
                        String startInfo = "\n\n<!-- WORKFLOW_START -->\n\n" +
                                "# \uD83D\uDE80 代码生成 Agent 启动\n\n" +
                                "<div class='workflow-meta'>\n\n" +
                                "| 项目 | 信息 |\n" +
                                "|------|------|\n" +
                                "| 产物ID | `" + appId + "` |\n" +
                                "| 生成类型 | **" + type.getValue() + "** |\n" +
                                "| 执行模式 | **分布式工作流** |\n" +
                                "| 原始需求 | " + (originalPrompt.length() > 100 ? originalPrompt.substring(0, 100) + "..." : originalPrompt) + " |\n\n" +
                                "</div>\n\n";
                        emitStreamBlock(sink, aiResponseBuilder, startInfo);
                        emitStreamBlock(sink, aiResponseBuilder, "\n\n<!-- WORKFLOW_EXECUTION_START -->\n\n" +
                                "## \uD83D\uDCCB 工作流执行计划\n\n" +
                                "<div class='workflow-steps'>\n\n" +
                                "**执行阶段:**\n\n" +
                                "1. \uD83C\uDF10 资源收集（网络资源 + 图片资源并行）\n" +
                                "2. ✨ 提示词智能增强\n" +
                                "3. \uD83C\uDFAF 代码类型策略分析\n" +
                                "4. \uD83D\uDCBB AI 代码生成\n" +
                                "5. \uD83D\uDD0D 代码质量审查\n" +
                                "6. \uD83C\uDFD7\uFE0F 项目构建部署\n\n" +
                                "</div>\n\n" +
                                "---\n\n");
                    }

                    // 生成可视化工作流图
                    GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
                    log.info("\n工作流图:\n{}", graph.content());

                    for (NodeOutput<MessagesState<String>> step : workflow.stream(
                            Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
                        
                        // 获取当前步骤的上下文
                        WorkflowContext currentContext = WorkflowContext.getContext(step.state());
                        if (currentContext != null) {
                            StringBuilder stepInfo = new StringBuilder();
                            String nodeName = step.node();
                            String stepTitle = currentContext.getCurrentStep();

                            // 使用结构化的步骤标记
                            stepInfo.append(String.format("\n\n<!-- NODE_START:%s -->\n\n", nodeName));
                            stepInfo.append(String.format("### ✅ %s\n\n", stepTitle));
                            stepInfo.append("<div class='node-result'>\n\n");
                            
                            // 根据节点类型输出特定信息，采用更清晰的展示格式
                            switch (nodeName) {
                                case "resource_collector":
                                    stepInfo.append("**🌐 资源收集节点（并行执行）**\n\n");
                                    String webResource = currentContext.getWebResourceListStr();
                                    String imageResource = currentContext.getImageListStr();
                                    if (StrUtil.isNotBlank(webResource) && !"无".equals(webResource.trim())) {
                                        stepInfo.append(String.format("- ✓ 网络资源整理完成：收集到 **%d** 字符\n", webResource.length()));
                                    } else {
                                        stepInfo.append("- ℹ️ 未找到相关网络资源或无需收集\n");
                                    }
                                    if (StrUtil.isNotBlank(imageResource) && !"无".equals(imageResource.trim())) {
                                        stepInfo.append(String.format("- ✓ 图片资源收集完成：获取到 **%d** 字符\n", imageResource.length()));
                                    } else {
                                        stepInfo.append("- ℹ️ 未找到相关图片资源或无需收集\n");
                                    }
                                    stepInfo.append("- ✓ 执行模式：网络资源 + 图片资源**并行收集**\n");
                                    break;
                                    
                                case "prompt_enhancer":
                                    stepInfo.append("**✨ 提示词增强节点**\n\n");
                                    if (StrUtil.isNotBlank(currentContext.getEnhancedPrompt())) {
                                        int length = currentContext.getEnhancedPrompt().length();
                                        stepInfo.append(String.format("- ✓ 增强完成：提示词优化至 **%d** 字符\n", length));
                                        stepInfo.append("- ✓ 增强内容：融合网络资源、图片信息\n");
                                        stepInfo.append("- ✓ 质量提升：更精准的代码生成指令\n");
                                    }
                                    break;
                                    
                                case "ai_code_generator_type_strategy":
                                    stepInfo.append("**🎯 代码类型策略节点**\n\n");
                                    if (currentContext.getGenerationType() != null) {
                                        String codeType = currentContext.getGenerationType().getValue();
                                        stepInfo.append(String.format("- ✓ 策略确定：选择 **%s** 生成模式\n", codeType));
                                        stepInfo.append("- ✓ AI分析：基于需求自动选择最优方案\n");
                                        stepInfo.append("- ✓ 准备就绪：开始代码生成流程\n");
                                    }
                                    break;
                                    
                                case "code_generator":
                                    stepInfo.append("**💻 AI 代码生成节点**\n\n");
                                    if (StrUtil.isNotBlank(currentContext.getOutputDir())) {
                                        stepInfo.append(String.format("- ✓ 生成完成：代码已输出\n"));
                                        stepInfo.append(String.format("- ✓ 输出路径：`%s`\n", currentContext.getOutputDir()));
                                        stepInfo.append("- ✓ 下一步：等待 AI 代码质量审查\n");
                                    }
                                    stepInfo.append("\n> 🔍 正在启动代码审查流程...\n");
                                    break;
                                    
                                case "ai_code_reviewer":
                                    stepInfo.append("**🔍 代码审查节点**\n\n");
                                    if (currentContext.getCodeReviewResponse() != null) {
                                        boolean passed = currentContext.getCodeReviewResponse().getIsPass();
                                        if (passed) {
                                            stepInfo.append("- ✅ **审查通过**：代码质量符合标准\n");
                                            stepInfo.append("- ✓ 代码规范：符合最佳实践\n");
                                            stepInfo.append("- ✓ 功能完整：满足需求描述\n");
                                        } else {
                                            stepInfo.append("- ⚠️ **审查未通过**：发现需要优化的问题\n");
                                            stepInfo.append("- 🔄 自动修复：正在重新生成优化代码\n");
                                            stepInfo.append("- 📝 改进方案：基于审查反馈调整\n");
                                        }
                                    }
                                    break;
                                    
                                case "project_builder":
                                    stepInfo.append("**🏗️ 项目构建节点**\n\n");
                                    if (StrUtil.isNotBlank(currentContext.getDeployDir())) {
                                        stepInfo.append("- ✓ 构建完成：项目已成功编译\n");
                                        stepInfo.append(String.format("- ✓ 部署目录：`%s`\n", currentContext.getDeployDir()));
                                        stepInfo.append("- ✓ 状态：可预览和部署\n");
                                    }
                                    break;

                                default:
                                    stepInfo.append("**⚙️ 工作流初始化**\n\n");
                                    stepInfo.append("- ✓ 工作流上下文已创建\n");
                                    stepInfo.append("- ✓ 执行参数已配置完成\n");
                                    stepInfo.append("- ✓ 准备开始执行各节点任务\n");
                                    break;
                            }
                            
                            // 错误信息总是显示，使用醒目的格式
                            if (StrUtil.isNotBlank(currentContext.getErrorMessage())) {
                                stepInfo.append("\n\n---\n\n");
                                stepInfo.append("**⚠️ 执行异常**\n\n");
                                stepInfo.append("```\n");
                                stepInfo.append(currentContext.getErrorMessage());
                                stepInfo.append("\n```\n");
                                stepInfo.append("\n> 💡 提示：异常详情已记录到服务端日志，请检查控制台输出\n");
                            }

                            stepInfo.append("\n\n</div>\n\n");
                            stepInfo.append(String.format("<!-- NODE_END:%s -->\n\n", nodeName));
                            
                            emitStreamBlock(sink, aiResponseBuilder, stepInfo.toString());
                            log.info("当前步骤: {} - {}", nodeName, currentContext.getCurrentStep());

                            // 如果是项目构建节点，说明是最后一步，主动结束循环，防止流迭代器阻塞
                            if ("project_builder".equals(nodeName)) {
                                log.info("项目构建完成，主动结束工作流循环");
                                break;
                            }
                        }
                    }

                    // 发送工作流完成事件 - 使用结构化完成标记
                    String completeInfo = "\n\n<!-- WORKFLOW_COMPLETE -->\n\n" +
                            "---\n\n" +
                            "# 🎉 代码生成任务完成\n\n" +
                            "<div class='workflow-summary'>\n\n" +
                            "**✅ 所有工作流节点执行完毕**\n\n" +
                            "- 🌐 网络资源已整理\n" +
                            "- 🖼️ 图片资源已收集\n" +
                            "- ✨ 提示词已增强\n" +
                            "- 🎯 代码类型已确定\n" +
                            "- 💻 代码已生成\n" +
                            "- 🔍 质量已审查\n" +
                            "- 🏗️ 项目已构建\n\n" +
                            "**下一步操作：**\n\n" +
                            "- 📱 切换到「产物模式」查看预览\n" +
                            "- 🚀 点击「部署」发布到线上\n" +
                            "- 💾 点击「下载」获取源代码\n\n" +
                            "</div>\n\n";
                    emitStreamBlock(sink, aiResponseBuilder, completeInfo);
                    
                    log.info("代码生成工作流执行完成！产物ID: {}", appId);
                    sink.complete();
                } catch (Throwable e) {
                    log.error("工作流执行失败，产物ID: {}，错误信息: {}", appId, e.getMessage(), e);
                    // 发送错误事件 - 使用结构化错误格式
                    String errorInfo = "\n\n<!-- WORKFLOW_ERROR -->\n\n" +
                            "---\n\n" +
                            "# 🛑 工作流执行异常\n\n" +
                            "<div class='workflow-error'>\n\n" +
                            "**异常类型：** " + e.getClass().getSimpleName() + "\n\n" +
                            "**错误信息：**\n\n" +
                            "```\n" +
                            e.getMessage() +
                            "\n```\n\n" +
                            "**处理建议：**\n\n" +
                            "1. 检查服务端控制台日志获取详细堆栈信息\n" +
                            "2. 确认AI服务配置是否正确（API Key等）\n" +
                            "3. 如问题持续，请联系技术支持\n\n" +
                            "</div>\n\n";

                    emitStreamBlock(sink, aiResponseBuilder, errorInfo);
                    sink.error(e);
                } finally {
                    // 确保流式输出发射器被注销，防止内存泄漏
                    CodeGeneratorNode.unregisterStreamEmitter(appId);
                }
            });
        });
        // 处理流，把收集 AI 响应内容保存到对话历史，并进一步处理为响应给前端的最终内容
        // 注意：不要在此处添加 doOnComplete 保存对话历史，CommonStreamHandler.doFinally 已统一处理保存逻辑
        return commonStreamHandler.handleStream(
                fluxStream
                        // 过滤空字串
                        .filter(StrUtil::isNotEmpty),
                chatHistoryService, appId, userId, aiResponseBuilder);
    }

    /**
     * 辅助方法：流式输出文本，模拟打字效果（仅用于需要打字效果的场景，如 AI 代码生成流）
     *
     * @param sink            SSE事件流
     * @param aiResponseBuilder 响应内容构建器
     * @param text            需要输出的文本
     */
    private void emitStreamText(FluxSink<String> sink, StringBuilder aiResponseBuilder, String text) {
        // 使用 codePoints() 遍历，正确处理 emoji 等 Unicode 补充字符（避免 surrogate pair 被拆分导致乱码）
        text.codePoints().forEach(codePoint -> {
            try {
                Thread.sleep(STREAM_CHAR_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            String ch = new String(Character.toChars(codePoint));
            sink.next(ch);
            aiResponseBuilder.append(ch);
        });
    }

    /**
     * 辅助方法：批量发送文本，一次性推送整段内容（用于工作流状态信息，无需模拟打字效果）
     * 相比 emitStreamText 逐字符发送，可节省大量延迟时间
     *
     * @param sink            SSE事件流
     * @param aiResponseBuilder 响应内容构建器
     * @param text            需要输出的文本
     */
    private void emitStreamBlock(FluxSink<String> sink, StringBuilder aiResponseBuilder, String text) {
        sink.next(text);
        aiResponseBuilder.append(text);
    }

    /**
     * 条件边的路由逻辑
     *
     * @param state 消息状态，包含工作流上下文信息
     * @return String 自定义的路由结果参数
     * @author DuRuiChi
     * @create 2026/1/14
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