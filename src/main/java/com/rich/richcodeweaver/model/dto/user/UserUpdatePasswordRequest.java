package com.rich.richcodeweaver.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户更新响应类
 *
 * @author DuRuiChi
 * @create 2025/8/5
 **/
@Data
public class UserUpdatePasswordRequest implements Serializable {

    /**
     * 用户 ID
     */
    private long userId;

    /**
     * 旧密码
     */
    private String oldPassword;

    /**
     * 新密码
     */
    private String newPassword;

    private static final long serialVersionUID = 1L;
}