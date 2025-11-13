package com.rich.common.exception;

import cn.hutool.json.JSONUtil;
import com.rich.common.model.BaseResponse;
import com.rich.common.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.Map;

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
        // 尝试处理 SSE 请求
        if (handleSseError(e.getCode(), e.getMessage())) {
            return null;
        }
        // 对于普通请求，返回标准 JSON 响应
        return ResultUtils.error(e.getCode(), e.getMessage().replaceAll(NON_CHINESE_CHAR_REGEX, ""));
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        // 尝试处理 SSE 请求
        if (handleSseError(ErrorCode.SYSTEM_ERROR.getCode(), e.getMessage())) {
            return null;
        }
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, e.getMessage().replaceAll(NON_CHINESE_CHAR_REGEX, ""));
    }

    /**
     * 判断是否为 SSE 请求，如果是则发送错误信息的流，如果不是则跳过
     *
     * @param errorCode    错误码
     * @param errorMessage 错误信息
     * @return true表示是SSE请求并已处理，false表示不是SSE请求
     */
    private boolean handleSseError(int errorCode, String errorMessage) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return false;
        }
        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();
        // 判断是否是SSE请求（通过Accept头或URL路径）
        String accept = request.getHeader("Accept");
        String uri = request.getRequestURI();
        // 如果是 SSE 请求，则发送错误信息的流，如果不是则跳过
        if ((accept != null && accept.contains("text/event-stream")) || uri.contains("/gen/code/stream")) {
            // 发送错误信息的流
            try {
                // 设置SSE响应头
                response.setContentType("text/event-stream");
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Cache-Control", "no-cache");
                response.setHeader("Connection", "keep-alive");
                // 构造错误消息的SSE格式
                Map<String, Object> errorData = Map.of(
                        "error", true,
                        "code", errorCode,
                        "message", errorMessage.replaceAll("[^\\u4e00-\\u9fa5]", "")
                );
                String errorJson = JSONUtil.toJsonStr(errorData);
                // 发送业务错误事件（避免与标准error事件冲突）
                String sseData = "event: business-error\ndata: " + errorJson + "\n\n";
                response.getWriter().write(sseData);
                response.getWriter().flush();
                // 发送结束事件
                response.getWriter().write("event: done\ndata: {}\n\n");
                response.getWriter().flush();
                // 表示已处理SSE请求
                return true;
            } catch (IOException ioException) {
                // 写入失败，不作处理，返回 true，代表是 SSE 请求
                log.error("Failed to write SSE error response", ioException);
                return true;
            }
        }
        return false;
    }
}