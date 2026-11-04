package com.rich.model.dto.collaborator;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 协作邀请处理请求
 * 被邀请者接受或拒绝协作邀请
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@Data
public class CollaboratorHandleRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 协作关系id
     */
    private Long id;

    /**
     * 处理动作: 1=接受, 2=拒绝
     */
    private Integer action;
}
