package com.rich.codeweaver.model.dto.chat;

import com.rich.codeweaver.common.model.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 聊天消息分页查询请求
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChatMessageQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话id
     */
    private Long conversationId;
}
