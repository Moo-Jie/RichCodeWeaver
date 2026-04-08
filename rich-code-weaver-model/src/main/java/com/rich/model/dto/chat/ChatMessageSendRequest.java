package com.rich.model.dto.chat;

import lombok.Data;

import java.io.Serializable;

/**
 * 发送聊天消息请求
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@Data
public class ChatMessageSendRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 接收方用户id
     */
    private Long receiverId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型: text=文本, image=图片
     */
    private String messageType;
}
