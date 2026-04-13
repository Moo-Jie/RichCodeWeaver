package com.rich.codeweaver.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户绑定手机号请求
 *
 * @author DuRuiChi
 * @since 2026-03-30
 */
@Data
public class UserBindPhoneRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 手机号
     */
    private String phone;
}
