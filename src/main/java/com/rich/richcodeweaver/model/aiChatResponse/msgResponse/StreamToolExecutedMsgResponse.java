package com.rich.richcodeweaver.model.aiChatResponse.msgResponse;

import com.rich.richcodeweaver.model.enums.ReasoningStreamMsgTypeEnum;
import dev.langchain4j.service.tool.ToolExecution;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 用于 AI 推理模型调用接口流式输出 【工具执行结果信息】 的响应结果
 *
 * @author DuRuiChi
 * @create 2025/8/25
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class StreamToolExecutedMsgResponse extends StreamMsgResponse {
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
     * 工具执行结果信息
     */
    private String result;

    /**
     * 工具执行结果信息
     */
    public StreamToolExecutedMsgResponse(ToolExecution toolExecution) {
        super(ReasoningStreamMsgTypeEnum.TOOL_EXECUTED.getValue());
        this.id = toolExecution.request().id();
        this.name = toolExecution.request().name();
        this.arguments = toolExecution.request().arguments();
        this.result = toolExecution.result();
    }

}
