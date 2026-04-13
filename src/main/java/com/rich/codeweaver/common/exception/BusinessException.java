package com.rich.codeweaver.common.exception;

import lombok.Getter;

/**
 * 业务异常类
 * 用于封装业务逻辑中的异常信息
 *
 * @author DuRuiChi
 * @since 2026-03-08
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
