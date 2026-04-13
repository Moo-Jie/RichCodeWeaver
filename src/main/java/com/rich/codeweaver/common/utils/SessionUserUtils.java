package com.rich.codeweaver.common.utils;

import java.lang.reflect.Method;

public final class SessionUserUtils {

    private SessionUserUtils() {
    }

    public static Long resolveUserId(Object sessionValue) {
        if (sessionValue == null) {
            return null;
        }
        if (sessionValue instanceof Long userId) {
            return userId;
        }
        if (sessionValue instanceof Number number) {
            return number.longValue();
        }
        try {
            Method getIdMethod = sessionValue.getClass().getMethod("getId");
            Object idValue = getIdMethod.invoke(sessionValue);
            if (idValue instanceof Long userId) {
                return userId;
            }
            if (idValue instanceof Number number) {
                return number.longValue();
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }
}
