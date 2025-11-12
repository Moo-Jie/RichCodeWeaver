package com.rich.richcodeweaver.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册响应类
 *
 * @author DuRuiChi
 * @create 2025/8/5
 **/
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;
}