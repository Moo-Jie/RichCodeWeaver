package com.rich.codeweaver.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天消息 实体类
 * 存储用户之间的聊天消息内容
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_message")
public class ChatMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 会话id
     */
    @Column("conversationId")
    private Long conversationId;

    /**
     * 发送方用户id
     */
    @Column("senderId")
    private Long senderId;

    /**
     * 接收方用户id
     */
    @Column("receiverId")
    private Long receiverId;

    /**
     * 消息内容
     */
    @Column("content")
    private String content;

    /**
     * 消息类型: text=文本, image=图片
     */
    @Column("messageType")
    private String messageType;

    /**
     * 是否已读: 0=未读, 1=已读
     */
    @Column("isRead")
    private Integer isRead;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;
}
