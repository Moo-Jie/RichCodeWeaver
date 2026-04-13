package com.rich.codeweaver.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 客服会话实体类
 *
 * @author DuRuiChi
 * @create 2026/4/9
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("customer_service_conversation")
public class CustomerServiceConversation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    @Column("title")
    private String title;

    @Column("userId")
    private Long userId;

    @Column("visitorKey")
    private String visitorKey;

    @Column("lastMessagePreview")
    private String lastMessagePreview;

    @Column("lastMessageTime")
    private LocalDateTime lastMessageTime;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
