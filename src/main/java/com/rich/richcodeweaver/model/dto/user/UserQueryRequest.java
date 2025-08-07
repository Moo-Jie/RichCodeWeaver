package com.rich.richcodeweaver.model.dto.user;

import com.rich.richcodeweaver.model.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 分页查询用户响应类
 *
 * @author DuRuiChi
 * @create 2025/8/5
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public final class UserQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin/ban
     */
    private String userRole;

    private static final long serialVersionUID = 1L;
}