package com.rich.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天消息 视图对象
 * 包含消息内容及发送方信息，用于前端聊天窗口展示
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@Data
public class ChatMessageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息id
     */
    private Long id;

    /**
     * 会话id
     */
    private Long conversationId;

    /**
     * 发送方用户id
     */
    private Long senderId;

    /**
     * 发送方用户昵称
     */
    private String senderName;

    /**
     * 发送方用户头像
     */
    private String senderAvatar;

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

    /**
     * 协作关系id（协作邀请消息专用）
     */
    private Long collabId;

    /**
     * 协作产物id（协作邀请消息专用）
     */
    private Long appId;

    /**
     * 协作产物名称（协作邀请消息专用）
     */
    private String appName;

    /**
     * 协作产物封面（协作邀请消息专用）
     */
    private String appCover;

    /**
     * 协作角色（协作邀请消息专用）
     */
    private String collabRole;

    /**
     * 协作状态（协作邀请消息专用）
     */
    private Integer collabStatus;

    /**
     * 是否已读: 0=未读, 1=已读
     */
    private Integer isRead;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
