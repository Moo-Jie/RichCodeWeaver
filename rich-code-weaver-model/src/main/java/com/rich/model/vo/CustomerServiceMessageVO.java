package com.rich.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 客服消息视图对象
 *
 * @author DuRuiChi
 * @create 2026/4/9
 */
@Data
public class CustomerServiceMessageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long conversationId;

    private String senderType;

    private String content;

    private LocalDateTime createTime;
}
