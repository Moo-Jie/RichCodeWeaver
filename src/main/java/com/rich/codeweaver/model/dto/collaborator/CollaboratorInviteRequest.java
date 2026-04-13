package com.rich.codeweaver.model.dto.collaborator;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 协作者邀请请求
 * 产物所有者邀请好友成为协作者
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@Data
public class CollaboratorInviteRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 产物id
     */
    private Long appId;

    /**
     * 被邀请的用户id
     */
    private Long userId;

    /**
     * 协作角色: editor(编辑者), viewer(查看者)，默认editor
     */
    private String role;
}
