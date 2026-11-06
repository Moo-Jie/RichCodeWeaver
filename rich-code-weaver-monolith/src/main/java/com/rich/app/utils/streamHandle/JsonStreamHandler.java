package com.rich.app.utils.streamHandle;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rich.ai.aiTools.BaseTool;
import com.rich.ai.aiTools.ToolsManager;
import com.rich.ai.model.msgResponse.StreamAiChatMsgResponse;
import com.rich.ai.model.msgResponse.StreamMsgResponse;
import com.rich.ai.model.msgResponse.StreamToolExecutedMsgResponse;
import com.rich.ai.model.msgResponse.StreamToolInvocMsgResponse;
import com.rich.app.service.ChatHistoryService;
import com.rich.common.constant.AiServiceConstant;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.model.enums.ReasoningStreamMsgTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;

/**
 * 用于 AI JSON 输出的流处理器。
 *
 * <p>后端在此处统一把推理模型的 ai_response、tool_request、tool_executed 转换为
 * 可展示 Markdown，前端不再维护工具名称、图标、参数摘要等业务映射。</p>
 *
 * @author DuRuiChi
 * @create 2025/12/27
 */
@Slf4j
@Component
public class JsonStreamHandler {

    /**
     * 不进入对话正文的低信息量内部工具。文件读写是执行细节，由阶段摘要和产物状态承载。
     */
    private static final Set<String> SILENT_TOOLS = Set.of(
            "think",
            "sendMessage",
            "exit",
            "creatAndWrite",
            "modifyFile",
            "readFile",
            "readDir",
            "diffFile",
            "deleteFile"
    );

    @Resource
    private CommonStreamHandler commonStreamHandler;

    @Resource
    private ToolsManager toolsManager;

    /**
     * 处理 AI JSON 输出并生成统一 SSE 流。
     *
     * @param stringFlux         AI 响应流
     * @param chatHistoryService 对话历史服务
     * @param appId              产物 ID
     * @param userId             用户 ID
     * @param summaryType        摘要类型
     * @return 处理后的 SSE 流
     */
    public Flux<ServerSentEvent<String>> handleStream(Flux<String> stringFlux,
                                                      ChatHistoryService chatHistoryService,
                                                      Long appId,
                                                      Long userId,
                                                      String summaryType) {
        StringBuilder aiResponseBuilder = new StringBuilder();
        Set<String> seenToolIds = new HashSet<>();

        String modeLabel = AiServiceConstant.SUMMARY_TYPE_WORKFLOW.equals(summaryType)
                ? "系统分步执行"
                : "Agent 智能生成";
        String header = ConversationOutputFormatter.documentHeader("生成过程", modeLabel);
        String completion = ConversationOutputFormatter.completion("可查看预览、部署或下载");
        aiResponseBuilder.append(header);

        Flux<String> displayFlux = stringFlux
                .map(chunk -> handleJsonChunk(chunk, aiResponseBuilder, seenToolIds))
                .filter(StrUtil::isNotEmpty);

        Flux<String> outputFlux = Flux.concat(
                Flux.just(header),
                displayFlux,
                Flux.just(completion).doOnNext(aiResponseBuilder::append)
        );

        return commonStreamHandler.handleStream(
                outputFlux,
                chatHistoryService,
                appId,
                userId,
                aiResponseBuilder,
                summaryType
        );
    }

    /**
     * Agent 模式解析入口，保留 AI 的最终自然语言回复。
     *
     * @param chunk       AI 响应块
     * @param seenToolIds 已见工具 ID
     * @return 可展示 Markdown
     */
    public String parseJsonChunk(String chunk, Set<String> seenToolIds) {
        return parseJsonChunk(chunk, seenToolIds, true);
    }

    /**
     * 工作流模式解析入口。工作流正文由阶段摘要承载，不展示模型过程性闲聊。
     *
     * @param chunk       AI 响应块
     * @param seenToolIds 已见工具 ID
     * @return 可展示 Markdown 或隐藏任务计划标记
     */
    public String parseWorkflowChunk(String chunk, Set<String> seenToolIds) {
        return parseJsonChunk(chunk, seenToolIds, false);
    }

    private String parseJsonChunk(String chunk, Set<String> seenToolIds, boolean includeAiResponse) {
        String trimmedChunk = StrUtil.trim(chunk);
        if (StrUtil.isEmpty(trimmedChunk)) {
            return "";
        }

        StreamMsgResponse streamMsgResponse = JSONUtil.toBean(trimmedChunk, StreamMsgResponse.class);
        ReasoningStreamMsgTypeEnum type = ReasoningStreamMsgTypeEnum.getEnumByValue(streamMsgResponse.getType());
        ThrowUtils.throwIf(type == null, ErrorCode.OPERATION_ERROR, "不支持的 AI 消息类型");

        return switch (type) {
            case AI_RESPONSE -> {
                StreamAiChatMsgResponse response = JSONUtil.toBean(trimmedChunk, StreamAiChatMsgResponse.class);
                yield includeAiResponse ? StrUtil.blankToDefault(response.getData(), "") : "";
            }
            case TOOL_REQUEST -> {
                StreamToolInvocMsgResponse request = JSONUtil.toBean(trimmedChunk, StreamToolInvocMsgResponse.class);
                if (StrUtil.isNotBlank(request.getId())) {
                    seenToolIds.add(request.getId());
                }
                yield "";
            }
            case TOOL_EXECUTED -> buildToolExecutedMarkdown(
                    JSONUtil.toBean(trimmedChunk, StreamToolExecutedMsgResponse.class));
        };
    }

    private String buildToolExecutedMarkdown(StreamToolExecutedMsgResponse executedMessage) {
        String toolName = StrUtil.blankToDefault(executedMessage.getName(), "unknown");
        String toolResult = executedMessage.getResult();
        JSONObject arguments = parseArguments(executedMessage.getArguments());

        if ("taskPlan".equals(toolName)) {
            return buildTaskPlanMarker(toolResult);
        }
        if (SILENT_TOOLS.contains(toolName)) {
            return "";
        }

        String displayName = getToolDisplayName(toolName);
        String summary = getToolExecutionSummary(toolName, arguments, toolResult);
        return ConversationOutputFormatter.toolCompleted(displayName, summary);
    }

    private JSONObject parseArguments(String arguments) {
        if (StrUtil.isBlank(arguments)) {
            return new JSONObject();
        }
        try {
            return JSONUtil.parseObj(arguments);
        } catch (Exception e) {
            log.debug("解析工具参数失败，使用空参数", e);
            return new JSONObject();
        }
    }

    private String getToolDisplayName(String toolName) {
        BaseTool tool = toolsManager.getToolByName(toolName);
        if (tool != null && StrUtil.isNotBlank(tool.getToolDisplayName())) {
            return tool.getToolDisplayName();
        }
        return toolName;
    }

    private String getToolExecutionSummary(String toolName, JSONObject arguments, String toolResult) {
        String summary = switch (toolName) {
            case "creatAndWrite", "modifyFile", "readFile", "deleteFile" ->
                    arguments.getStr("relativeFilePath");
            case "readDir" -> StrUtil.blankToDefault(
                    arguments.getStr("relativeDirPath"),
                    StrUtil.blankToDefault(arguments.getStr("dirPath"), "."));
            case "diffFile" -> buildDiffSummary(arguments);
            case "searchWeb", "searchImages", "searchCodeExample" -> arguments.getStr("query");
            case "scrapeWebPage" -> StrUtil.blankToDefault(
                    arguments.getStr("scrapUrl"),
                    StrUtil.blankToDefault(arguments.getStr("ScrapUrl"), arguments.getStr("url")));
            case "aiGeneratorImage" -> ConversationOutputFormatter.safeInline(
                    StrUtil.blankToDefault(arguments.getStr("description"), arguments.getStr("type")));
            case "setCodeGenType" -> arguments.getStr("codeGenType");
            case "buildProject" -> StrUtil.isNotBlank(toolResult)
                    ? firstLine(toolResult, 100)
                    : "Vue 项目构建";
            case "validateProject" -> StrUtil.isNotBlank(toolResult)
                    ? firstLine(toolResult, 100)
                    : "产物结构验证";
            case "getUserProfile" -> "读取当前用户画像";
            case "updateUserProfile" -> "更新当前用户画像";
            default -> StrUtil.isNotBlank(toolResult) ? firstLine(toolResult, 100) : "";
        };
        return ConversationOutputFormatter.clip(summary, 120);
    }

    private String buildDiffSummary(JSONObject arguments) {
        String left = arguments.getStr("filePath1");
        String right = arguments.getStr("filePath2");
        if (StrUtil.isNotBlank(left) || StrUtil.isNotBlank(right)) {
            return StrUtil.blankToDefault(left, "文件") + " / " + StrUtil.blankToDefault(right, "文件");
        }
        return "内容差异对比";
    }

    private String buildTaskPlanMarker(String toolResult) {
        if (StrUtil.isBlank(toolResult)) {
            return "";
        }
        int markerIndex = toolResult.lastIndexOf("[任务计划]");
        if (markerIndex < 0) {
            return "";
        }
        String taskPlanJson = toolResult.substring(markerIndex + "[任务计划]".length()).trim();
        if (StrUtil.isBlank(taskPlanJson)) {
            return "";
        }
        try {
            JSONUtil.parseObj(taskPlanJson);
            return ConversationOutputFormatter.taskPlanMarker(taskPlanJson);
        } catch (Exception e) {
            log.debug("任务计划 JSON 解析失败，跳过隐藏标记", e);
            return "";
        }
    }

    private String firstLine(String value, int maxLength) {
        String normalized = value.replace("\r", "\n");
        int lineEnd = normalized.indexOf('\n');
        String line = lineEnd >= 0 ? normalized.substring(0, lineEnd) : normalized;
        return ConversationOutputFormatter.clip(line, maxLength);
    }

    private String handleJsonChunk(String chunk, StringBuilder aiResponseBuilder, Set<String> seenToolIds) {
        String result = parseJsonChunk(chunk, seenToolIds);
        if (StrUtil.isNotEmpty(result) && !ConversationOutputFormatter.isTransientMarker(result)) {
            aiResponseBuilder.append(result);
        }
        return result;
    }
}
