package com.rich.codeweaver.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 客服会话视图对象
 *
 * @author DuRuiChi
 * @create 2026/4/9
 */
@Data
public class CustomerServiceConversationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String title;

    private String lastMessagePreview;

    private LocalDateTime lastMessageTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
