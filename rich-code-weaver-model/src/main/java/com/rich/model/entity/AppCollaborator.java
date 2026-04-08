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
 * 产物协作者 实体类
 * 记录产物的协作者关系，支持邀请、接受、拒绝、移除等状态流转
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("app_collaborator")
public class AppCollaborator implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 产物id
     */
    @Column("appId")
    private Long appId;

    /**
     * 协作者用户id
     */
    @Column("userId")
    private Long userId;

    /**
     * 邀请人用户id（产物所有者）
     */
    @Column("inviterId")
    private Long inviterId;

    /**
     * 状态: 0=待确认, 1=已接受, 2=已拒绝, 3=已移除
     */
    private Integer status;

    /**
     * 协作角色: editor(编辑者), viewer(查看者)
     */
    private String role;

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
