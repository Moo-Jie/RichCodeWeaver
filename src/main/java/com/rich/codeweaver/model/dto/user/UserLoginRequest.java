package com.rich.codeweaver.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求
 *
 * @author DuRuiChi
 * @create 2025/12/5
 **/
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 密码
     */
    private String userPassword;
}