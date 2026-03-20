package com.rich.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户更新响应类
 *
 * @author DuRuiChi
 * @create 2025/8/5
 **/
@Data
public class UserUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;
    /**
     * 用户昵称
     */
    private String userName;
    /**
     * 用户头像
     */
    private String userAvatar;
    /**
     * 简介
     */
    private String userProfile;
    /**
     * 用户角色：user/admin
     */
    private String userRole;
    /**
     * 用户身份：individual(个体)/merchant(商户)/enterprise(企业)
     */
    private String userIdentity;
    /**
     * 用户行业领域
     */
    private String userIndustry;
}