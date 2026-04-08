package com.rich.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 好友关系 视图对象
 * 包含好友的基本信息，用于前端展示
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@Data
public class UserFriendshipVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 好友关系id
     */
    private Long id;

    /**
     * 发起方用户id
     */
    private Long userId;

    /**
     * 接收方用户id
     */
    private Long friendId;

    /**
     * 状态: 0=待处理, 1=已同意, 2=已拒绝
     */
    private Integer status;

    /**
     * 申请备注
     */
    private String remark;

    /**
     * 对方用户昵称
     */
    private String friendName;

    /**
     * 对方用户头像
     */
    private String friendAvatar;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
