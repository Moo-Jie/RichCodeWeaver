package com.rich.common.exception;

import java.util.Collection;

/**
 * 异常工具类
 *
 * @author DuRuiChi
 * @create 2025/8/4
 **/
public class ThrowUtils {

    /**
     * 参数校验（非空）
     *
     * @param obj       待校验对象
     * @param errorCode 错误码
     * @author DuRuiChi
     * @create 2025/8/4
     */
    public static void requireNonNull(Object obj, ErrorCode errorCode) {
        throwIf(obj == null, errorCode);
    }

    /**
     * 集合非空校验
     *
     * @param collection 待校验集合
     * @param errorCode  错误码
     * @author DuRuiChi
     * @create 2025/8/4
     */
    public static void requireNonEmpty(Collection<?> collection, ErrorCode errorCode) {
        throwIf(collection == null || collection.isEmpty(), errorCode);
    }

    /**
     * 数值范围校验
     *
     * @param value     待校验数值
     * @param min       最小值
     * @param max       最大值
     * @param errorCode 错误码
     * @author DuRuiChi
     * @create 2025/8/4
     */
    public static void checkInRange(int value, int min, int max, ErrorCode errorCode) {
        throwIf(value < min || value > max, errorCode);
    }

    /**
     * 字符串非空校验
     *
     * @param str       待校验字符串
     * @param errorCode 错误码
     * @author DuRuiChi
     * @create 2025/8/4
     */
    public static void requireNonBlank(String str, ErrorCode errorCode) {
        throwIf(str == null || str.trim().isEmpty(), errorCode);
    }

    /**
     * 状态校验
     *
     * @param valid     状态是否有效
     * @param errorCode 错误码
     * @author DuRuiChi
     * @create 2025/8/4
     */
    public static void checkState(boolean valid, ErrorCode errorCode) {
        throwIf(!valid, errorCode);
    }

    /**
     * 条件抛出异常
     *
     * @param condition        条件
     * @param runtimeException 异常
     * @return void
     * @author DuRuiChi
     * @create 2025/8/4
     **/
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * 条件抛出异常
     *
     * @param condition 条件
     * @param errorCode 错误码
     * @return void
     * @author DuRuiChi
     * @create 2025/8/4
     **/
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        throwIf(condition, new BusinessException(errorCode));
    }

    /**
     * 条件抛出异常
     *
     * @param condition 条件
     * @param errorCode 错误码
     * @param message   错误信息
     * @return void
     * @author DuRuiChi
     * @create 2025/8/4
     **/
    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        throwIf(condition, new BusinessException(errorCode, message));
    }
}
