package com.rich.ai.model.msgResponse;

import com.rich.model.enums.ReasoningStreamMsgTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 用于 AI 推理模型调用接口流式输出 【AI 回复内容信息】 的响应结果
 *
 * @author DuRuiChi
 * @create 2025/8/25
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class StreamAiChatMsgResponse extends StreamMsgResponse {
    /**
     * 消息内容
     */
    private String data;

    /**
     * 构造为 AI 响应消息类型
     **/
    public StreamAiChatMsgResponse(String data) {
        super(ReasoningStreamMsgTypeEnum.AI_RESPONSE.getValue());
        this.data = data;
    }
}
