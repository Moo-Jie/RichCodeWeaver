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
 * 好友关系 实体类
 * 记录用户之间的好友申请与关系状态
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_friendship")
public class UserFriendship implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 发起方用户id
     */
    @Column("userId")
    private Long userId;

    /**
     * 接收方用户id
     */
    @Column("friendId")
    private Long friendId;

    /**
     * 状态: 0=待处理, 1=已同意, 2=已拒绝
     */
    @Column("status")
    private Integer status;

    /**
     * 申请备注
     */
    @Column("remark")
    private String remark;

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
