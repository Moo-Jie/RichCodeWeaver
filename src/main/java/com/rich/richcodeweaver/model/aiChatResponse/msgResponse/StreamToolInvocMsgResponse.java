package com.rich.richcodeweaver.model.aiChatResponse.msgResponse;

import com.rich.richcodeweaver.model.enums.ReasoningStreamMsgTypeEnum;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 用于 AI 推理模型调用接口流式输出 【工具调用信息】 的响应结果
 *
 * @author DuRuiChi
 * @create 2025/8/25
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class StreamToolInvocMsgResponse extends StreamMsgResponse {
    /**
     * 工具调用 ID
     */
    private String id;

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具参数信息
     */
    private String arguments;

    /**
     * 指定为工具请求信息类型
     *
     * @param toolExecutionRequest  工具执行请求
     **/
    public StreamToolInvocMsgResponse(ToolExecutionRequest toolExecutionRequest) {
        super(ReasoningStreamMsgTypeEnum.TOOL_REQUEST.getValue());
        this.id = toolExecutionRequest.id();
        this.name = toolExecutionRequest.name();
        this.arguments = toolExecutionRequest.arguments();
    }
}
