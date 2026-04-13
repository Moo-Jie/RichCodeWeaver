package com.rich.codeweaver.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 发送邮箱验证码请求
 *
 * @author DuRuiChi
 */
@Data
public class SendEmailCodeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 目标邮箱
     */
    private String email;

    /**
     * 图形验证码ID
     */
    private String captchaId;

    /**
     * 图形验证码计算结果
     */
    private String captchaAnswer;
}
