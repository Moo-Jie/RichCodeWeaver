package com.rich.model.dto.friendship;

import lombok.Data;

import java.io.Serializable;

/**
 * 好友申请处理请求（同意/拒绝）
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@Data
public class FriendHandleRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 好友关系id
     */
    private Long id;

    /**
     * 处理动作: 1=同意, 2=拒绝
     */
    private Integer action;
}
