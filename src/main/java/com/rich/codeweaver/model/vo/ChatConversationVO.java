package com.rich.codeweaver.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天会话 视图对象
 * 包含会话对方的用户信息及最新消息摘要，用于前端会话列表展示
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@Data
public class ChatConversationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话id
     */
    private Long id;

    /**
     * 对方用户id
     */
    private Long targetUserId;

    /**
     * 对方用户昵称
     */
    private String targetUserName;

    /**
     * 对方用户头像
     */
    private String targetUserAvatar;

    /**
     * 最后一条消息内容预览
     */
    private String lastMessageContent;

    /**
     * 最后一条消息类型: text, collab_invite 等
     */
    private String lastMessageType;

    /**
     * 最后一条消息时间
     */
    private LocalDateTime lastMessageTime;

    /**
     * 未读消息数
     */
    private Integer unreadCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
