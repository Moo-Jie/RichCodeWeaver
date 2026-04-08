package com.rich.model.dto.friendship;

import lombok.Data;

import java.io.Serializable;

/**
 * 好友申请请求
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@Data
public class FriendAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 目标用户id
     */
    private Long friendId;

    /**
     * 申请备注
     */
    private String remark;
}
