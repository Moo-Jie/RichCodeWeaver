package com.rich.model.entity;

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
 * 聊天会话 实体类
 * 记录两个用户之间的对话会话元数据
 * userIdSmall < userIdLarge 保证唯一性
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_conversation")
public class ChatConversation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 用户id(较小值)
     */
    @Column("userIdSmall")
    private Long userIdSmall;

    /**
     * 用户id(较大值)
     */
    @Column("userIdLarge")
    private Long userIdLarge;

    /**
     * 最后一条消息id
     */
    @Column("lastMessageId")
    private Long lastMessageId;

    /**
     * 最后一条消息时间
     */
    @Column("lastMessageTime")
    private LocalDateTime lastMessageTime;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column("updateTime")
    private LocalDateTime updateTime;
}
