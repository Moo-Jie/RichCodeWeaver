package com.rich.richcodeweaver.utils.aiUtils.streamHandle;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rich.richcodeweaver.aiTools.BaseTool;
import com.rich.richcodeweaver.aiTools.ToolsManager;
import com.rich.richcodeweaver.constant.AppConstant;
import com.rich.richcodeweaver.exception.ThrowUtils;
import com.rich.richcodeweaver.model.aiChatResponse.msgResponse.StreamAiChatMsgResponse;
import com.rich.richcodeweaver.model.aiChatResponse.msgResponse.StreamMsgResponse;
import com.rich.richcodeweaver.model.aiChatResponse.msgResponse.StreamToolExecutedMsgResponse;
import com.rich.richcodeweaver.model.aiChatResponse.msgResponse.StreamToolInvocMsgResponse;
import com.rich.richcodeweaver.model.enums.ChatHistoryTypeEnum;
import com.rich.richcodeweaver.model.enums.ReasoningStreamMsgTypeEnum;
import com.rich.richcodeweaver.service.ChatHistoryService;
import com.rich.richcodeweaver.utils.deployWebProjectUtils.BuildWebProjectExecutor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;

import static com.rich.richcodeweaver.exception.ErrorCode.OPERATION_ERROR;

/**
 * 用于 AI JSON 输出的流处理器
 * (用于推理构建 Vue 项目模式的 AI 响应流处理器，因为推理模型关于 【思考步骤】 和 【工具调用】 等信息，是用 JOSN 格式输出的)
 *
 * @author DuRuiChi
 * @create 2025/8/27
 **/
@Slf4j
@Component
public class JsonStreamHandler {
    @Resource
    private CommonStreamHandler commonStreamHandler;

    @Resource
    private BuildWebProjectExecutor buildWebProjectExecutor;

    @Resource
    private ToolsManager toolsManager;

    /**
     * 处理 AI JSON 输出的流
     *
     * @param stringFlux         AI 响应流
     * @param chatHistoryService 对话历史服务
     * @param appId              应用 ID
     * @param userId             用户 ID
     * @return 处理后的 AI 响应流
     */
    public Flux<ServerSentEvent<String>> handleStream(Flux<String> stringFlux, ChatHistoryService chatHistoryService, Long appId, Long userId) {
        // 收集 AI 响应内容，用于保存到对话历史
        StringBuilder aiResponseBuilder = new StringBuilder();
        // 用于跟踪已经见过的工具ID，判断是否是第一次调用
        Set<String> seenToolIds = new HashSet<>();
        // 处理 AI 响应流
        return commonStreamHandler.handleStream(
                stringFlux
                        // 收集并解析 AI 响应内容的 TokenStream 数据块，用于保存到对话历史
                        .map(chunk -> {
                            // 解析每个 JSON 消息块
                            return handleJsonChunk(chunk, aiResponseBuilder, seenToolIds);
                        })
                        // 过滤空字串
                        .filter(StrUtil::isNotEmpty)
                        // 流结束后
                        .doOnComplete(() -> {
                            // 保存 AI 响应到对话历史
                            String aiResponse = aiResponseBuilder.toString();
                            if (StrUtil.isNotBlank(aiResponse)) {
                                chatHistoryService.addChatMessage(appId, aiResponse,
                                        ChatHistoryTypeEnum.AI.getValue(),
                                        userId);
                            }
                            // 代码生成完毕后，异步通过 npm 构造 Web 工程项目
                            String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
                            // 此处前端注意应访问 /dist 目录，
                            boolean isBuild = buildWebProjectExecutor.buildProjectAsync(projectPath);
                            ThrowUtils.throwIf(!isBuild, OPERATION_ERROR, "构建 Vue 项目失败");
                        }),
                chatHistoryService,
                appId,
                userId,
                aiResponseBuilder);
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
        // 移除可能的空白字符并检查是否为空
        String trimmedChunk = StrUtil.trim(chunk);
        if (StrUtil.isEmpty(trimmedChunk)) {
            return "";
        }
        // 解析 JSON 字符串为基础消息对象
        StreamMsgResponse streamMsgResponse = JSONUtil.toBean(chunk, StreamMsgResponse.class);

        // 获取类型
        ReasoningStreamMsgTypeEnum type = ReasoningStreamMsgTypeEnum.getEnumByValue(streamMsgResponse.getType());
        ThrowUtils.throwIf(type == null, OPERATION_ERROR, "不支持的 AI 消息类型");

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
                String data = streamAiChatMsgResponse.getData();
                // 直接将 AI 回复内容加入到对话历史
                aiResponseBuilder.append(data);
                return data;
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
                    BaseTool tool = toolsManager.getToolByName(streamToolInvocMsgResponse.getName());
                    String data = tool.getResponseMsg();
                    // 加入到对话历史
                    aiResponseBuilder.append(data);
                    return data;
                } else {
                    // 非首次调用，返回空字符串避免重复提示
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
                // 获取工具执行结果
                String data = String.format( "\n\n%s\n\n",tool.getResultMsg(argumentsJson));
                // 将 AI 回复内容加入到对话历史
                aiResponseBuilder.append(data);
                return data;
            }
            default -> {
                log.error("不支持的消息类型: {}", type);
                return "";
            }
        }
    }
}
