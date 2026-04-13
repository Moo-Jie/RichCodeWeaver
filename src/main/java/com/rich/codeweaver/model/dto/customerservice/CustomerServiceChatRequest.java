package com.rich.codeweaver.model.dto.customerservice;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 客服流式对话请求
 *
 * @author DuRuiChi
 * @create 2026/4/9
 */
@Data
public class CustomerServiceChatRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long conversationId;

    private String message;

    private Boolean reconnect;

    private String lastEventId;
}
