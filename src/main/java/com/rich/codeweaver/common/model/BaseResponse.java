package com.rich.codeweaver.common.model;

import com.rich.codeweaver.common.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 基础响应类
 * 统一封装API响应结果
 *
 * @author DuRuiChi
 * @since 2026-03-08
 */
@Data
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
