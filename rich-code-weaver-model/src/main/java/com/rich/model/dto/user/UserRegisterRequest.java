package com.rich.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求
 *
 * @author DuRuiChi
 * @create 2025/12/5
 **/
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 邮箱验证码
     */
    private String emailCode;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;
}