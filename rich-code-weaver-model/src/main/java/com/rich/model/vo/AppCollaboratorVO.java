package com.rich.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 产物协作者 视图对象
 * 包含协作者的用户信息和产物信息，用于前端展示
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@Data
public class AppCollaboratorVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 协作关系id
     */
    private Long id;

    /**
     * 产物id
     */
    private Long appId;

    /**
     * 产物名称
     */
    private String appName;

    /**
     * 产物封面
     */
    private String appCover;

    /**
     * 协作者用户id
     */
    private Long userId;

    /**
     * 协作者用户名
     */
    private String userName;

    /**
     * 协作者头像
     */
    private String userAvatar;

    /**
     * 邀请人用户id
     */
    private Long inviterId;

    /**
     * 邀请人用户名
     */
    private String inviterName;

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
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
