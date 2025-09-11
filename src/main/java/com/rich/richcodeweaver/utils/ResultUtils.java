package com.rich.richcodeweaver.utils;

import com.rich.richcodeweaver.exception.ErrorCode;
import com.rich.richcodeweaver.model.common.BaseResponse;

/**
 * 响应构建工具类
 *
 * @author DuRuiChi
 * @create 2025/8/4
 **/
public class ResultUtils {

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @return com.rich.richcodeweaver.common.response.BaseResponse<T>
     * @author DuRuiChi
     * @create 2025/8/4
     **/
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "ok");
    }

    /**
     * 失败响应（错误枚举）
     *
     * @param errorCode 错误枚举
     * @return com.rich.richcodeweaver.common.response.BaseResponse<?>
     * @author DuRuiChi
     * @create 2025/8/4
     **/
    public static BaseResponse<?> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     * 失败响应(错误码、信息)
     *
     * @param code    错误码
     * @param message 信息
     * @return com.rich.richcodeweaver.common.response.BaseResponse<?>
     * @author DuRuiChi
     * @create 2025/8/4
     **/
    public static BaseResponse<?> error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }

    /**
     * 失败响应(错误枚举、信息)
     *
     * @param errorCode 错误枚举
     * @param message   信息
     * @return com.rich.richcodeweaver.common.response.BaseResponse<?>
     * @author DuRuiChi
     * @create 2025/8/4
     **/
    public static BaseResponse<?> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), null, message);
    }
}