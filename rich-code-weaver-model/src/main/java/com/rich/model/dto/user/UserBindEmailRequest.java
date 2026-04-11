package com.rich.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户绑定邮箱请求
 *
 * @author DuRuiChi
 * @since 2026-03-30
 */
@Data
public class UserBindEmailRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 邮箱验证码
     */
    private String emailCode;
}
