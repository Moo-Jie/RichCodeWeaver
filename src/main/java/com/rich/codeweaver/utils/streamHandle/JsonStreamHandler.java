package com.rich.codeweaver.utils.streamHandle;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rich.codeweaver.aiTools.BaseTool;
import com.rich.codeweaver.aiTools.ToolsManager;
import com.rich.codeweaver.common.exception.ErrorCode;
import com.rich.codeweaver.common.exception.ThrowUtils;
import com.rich.codeweaver.model.dto.generator.msgResponse.StreamAiChatMsgResponse;
import com.rich.codeweaver.model.dto.generator.msgResponse.StreamMsgResponse;
import com.rich.codeweaver.model.dto.generator.msgResponse.StreamToolExecutedMsgResponse;
import com.rich.codeweaver.model.dto.generator.msgResponse.StreamToolInvocMsgResponse;
import com.rich.codeweaver.model.enums.ReasoningStreamMsgTypeEnum;
import com.rich.codeweaver.service.generator.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;

/**
 * 用于 AI JSON 输出的流处理器
 * (用于推理构建 Vue 项目模式的 AI 响应流处理器，因为推理模型关于 【思考步骤】 和 【工具调用】 等信息，是用 JOSN 格式输出的)
 *
 * @author DuRuiChi
 * @create 2025/12/27
 **/
@Slf4j
@Component
public class JsonStreamHandler {
    /**
     * MCP 工具或未注册本地工具的兜底开始提示模板
     **/
    private static final String DEFAULT_TOOL_START_TEMPLATE = "\n\n[开始调用系统工具] %s\n\n";

    /**
     * MCP 工具或未注册本地工具的兜底结束提示模板
     **/
    private static final String DEFAULT_TOOL_END_TEMPLATE = "[工具调用结束] %s";

    @Resource
    private CommonStreamHandler commonStreamHandler;

    @Resource
    private ToolsManager toolsManager;

    /**
     * 处理 AI JSON 输出的流
     *
     * @param stringFlux         AI 响应流
     * @param chatHistoryService 对话历史服务
     * @param appId              产物 ID
     * @param userId             用户 ID
     * @return 处理后的 AI 响应流
     */
    public Flux<ServerSentEvent<String>> handleStream(Flux<String> stringFlux, ChatHistoryService chatHistoryService, Long appId, Long userId) {
        // 收集 AI 响应内容，用于保存到对话历史
        StringBuilder aiResponseBuilder = new StringBuilder();

        // 用于跟踪已经见过的工具ID，判断是否是第一次调用
        // 因为同一个工具调用在流式输出中可能会拆成多段 tool_request 消息
        Set<String> seenToolIds = new HashSet<>();

        // 处理 AI 响应流
        // 注意：不要在此处添加 doOnComplete 保存对话历史，CommonStreamHandler.doFinally 已统一处理保存逻辑
        return commonStreamHandler.handleStream(
                stringFlux
                        // 收集并解析 AI 响应内容的 TokenStream 数据块，用于保存到对话历史
                        .map(chunk -> {
                            // 解析每个 JSON 消息块
                            return handleJsonChunk(chunk, aiResponseBuilder, seenToolIds);
                        })
                        // 过滤空字串
                        .filter(StrUtil::isNotEmpty),
                chatHistoryService,
                appId,
                userId,
                aiResponseBuilder);
    }

    /**
     * 解析 AI 响应流（TokenStream）中的 JSON 消息块，根据消息类型提取有效内容
     * （公共方法，供外部调用，如工作流模式下的流式输出发射器）
     *
     * @param chunk       AI 响应内容的 TokenStream 数据块
     * @param seenToolIds 已经见过的工具 ID 集合
     * @return java.lang.String 解析后的有效文本内容
     **/
    public String parseJsonChunk(String chunk, Set<String> seenToolIds) {
        // 移除可能的空白字符并检查是否为空
        String trimmedChunk = StrUtil.trim(chunk);
        if (StrUtil.isEmpty(trimmedChunk)) {
            return "";
        }
        // 解析 JSON 字符串为基础消息对象
        StreamMsgResponse streamMsgResponse = JSONUtil.toBean(chunk, StreamMsgResponse.class);

        // 获取类型
        // type 决定当前 chunk 是普通文本、工具调用开始，还是工具执行结束
        ReasoningStreamMsgTypeEnum type = ReasoningStreamMsgTypeEnum.getEnumByValue(streamMsgResponse.getType());
        ThrowUtils.throwIf(type == null, ErrorCode.OPERATION_ERROR, "不支持的 AI 消息类型");

        /*
            根据消息类型进行不同的流处理：
            type="ai_response", data="接下来我将开始创建代码："}

            {type="tool_request", index=xxx, id="xxx", name="CreatAndWriteAiTool", arguments="调用工具的参数（流式碎片）"}
            {type="tool_request", index=xxx, id="xxx", name="CreatAndWriteAiTool", arguments="调用工具的参数（流式碎片）"}
            .
            .
            .
            {type="tool_request", index=xxx, id="xxx", name="CreatAndWriteAiTool", arguments="调用工具的参数（流式碎片）"}

            {type="tool_executed", index=xxx, id="xxx", name="CreatAndWriteAiTool", arguments="文件写入完成！"}

            {type="ai_response", data="本次代码生成结束！"}
        */
        switch (type) {
            // AI 响应信息
            case AI_RESPONSE -> {
                // 转为用于 【AI 回复内容信息】 的响应结果类
                StreamAiChatMsgResponse streamAiChatMsgResponse = JSONUtil.toBean(chunk, StreamAiChatMsgResponse.class);
                return streamAiChatMsgResponse.getData();
            }
            // 工具请求信息
            case TOOL_REQUEST -> {
                // 转为用于 【工具调用信息】 的响应结果类
                StreamToolInvocMsgResponse streamToolInvocMsgResponse = JSONUtil.toBean(chunk, StreamToolInvocMsgResponse.class);
                // 获取工具 ID
                String toolId = streamToolInvocMsgResponse.getId();

                // 构建输出结果
                // 当第一次调用工具时，返回工具开始调用的消息
                if (toolId != null && !seenToolIds.contains(toolId)) {
                    // 记录工具 ID，防止重复输出开始调用的消息
                    seenToolIds.add(toolId);

                    // 构建输出结果
                    // 先尝试按本地工具名查找，因为原有工具都在 ToolsManager 中注册
                    BaseTool tool = toolsManager.getToolByName(streamToolInvocMsgResponse.getName());
                    if (tool != null) {
                        // 本地工具走原有展示逻辑，保证前端文案与历史行为一致
                        return tool.getResponseMsg();
                    }

                    // MCP 工具不在本地 ToolsManager 中注册时，使用通用提示文案兜底
                    // 这样即使是外部 MCP 工具，前端也能看到“开始调用工具”的过程提示
                    return String.format(DEFAULT_TOOL_START_TEMPLATE, streamToolInvocMsgResponse.getName());
                } else {
                    // 非首次调用，返回空字符串避免重复提示
                    // 因为后续碎片通常只是补充 arguments，不需要反复提示“开始调用工具”
                    return "";
                }
            }
            // 工具执行结果信息
            case TOOL_EXECUTED -> {
                // 转为用于 【工具执行结果信息】 的响应结果类
                StreamToolExecutedMsgResponse streamToolExecutedMsgResponse = JSONUtil.toBean(chunk, StreamToolExecutedMsgResponse.class);
                // 获取工具
                BaseTool tool = toolsManager.getToolByName(streamToolExecutedMsgResponse.getName());
                // 解析 Arguments 属性为 JSON 对象
                JSONObject argumentsJson = JSONUtil.parseObj(streamToolExecutedMsgResponse.getArguments());

                // 获取工具执行结果（传递实际的工具执行结果，供部分工具提取信息）
                String toolResult = streamToolExecutedMsgResponse.getResult();
                if (tool != null) {
                    // 本地工具继续复用原有 resultMsg 渲染逻辑，保证展示内容不变
                    return String.format("\n\n%s\n\n", tool.getResultMsg(argumentsJson, toolResult));
                }

                // MCP 工具执行结束时，优先展示工具返回结果；没有结果时再展示通用结束文案
                // 这样既兼容真正返回文本结果的 MCP 工具，也兼容只返回状态的工具
                String toolName = streamToolExecutedMsgResponse.getName();
                String fallbackMsg = StrUtil.isNotBlank(toolResult)
                        ? toolResult
                        : String.format(DEFAULT_TOOL_END_TEMPLATE, toolName);
                return String.format("\n\n%s\n\n", fallbackMsg);
            }
            default -> {
                log.error("不支持的消息类型: {}", type);
                return "";
            }
        }
    }

    /**
     * 处理 AI响应流（TokenStream）中的 JSON 消息块，根据消息类型执行不同的消息处理逻辑
     *
     * @param chunk             AI 响应内容的 TokenStream 数据块
     * @param aiResponseBuilder 对话历史字符串构建器
     * @param seenToolIds       已经见过的工具 ID 集合
     * @return java.lang.String
     **/
    private String handleJsonChunk(String chunk, StringBuilder aiResponseBuilder, Set<String> seenToolIds) {
        // 先解析出当前 chunk 对应的最终可展示文本
        String result = parseJsonChunk(chunk, seenToolIds);
        if (StrUtil.isNotEmpty(result)) {
            // 只有真正展示给前端的内容，才累计到对话历史里
            aiResponseBuilder.append(result);
        }
        return result;
    }
}
