package com.rich.richcodeweaver.exception;

import com.rich.richcodeweaver.model.common.BaseResponse;
import com.rich.richcodeweaver.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author DuRuiChi
 * @create 2025/8/4
 **/
@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    /**
     * 匹配非中文字符的正则表达式
     */
    private static final String NON_CHINESE_CHAR_REGEX = "[^\\u4e00-\\u9fa5]";

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        // 对于普通请求，返回标准 JSON 响应
        return ResultUtils.error(e.getCode(), e.getMessage().replaceAll(NON_CHINESE_CHAR_REGEX, ""));
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, e.getMessage().replaceAll(NON_CHINESE_CHAR_REGEX, ""));
    }
}