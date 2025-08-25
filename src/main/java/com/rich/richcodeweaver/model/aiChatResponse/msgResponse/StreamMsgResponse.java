package com.rich.richcodeweaver.model.aiChatResponse.msgResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用于 AI 推理模型调用接口流式输出内容的响应结果
 *
 * @author DuRuiChi
 * @create 2025/8/25
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StreamMsgResponse {
    /**
     * 消息类型
     * （即 ReasoningStreamMsgTypeEnum 类型的 value 值）
     */
    private String type;
}
