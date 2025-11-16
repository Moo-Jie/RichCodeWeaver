package com.rich.app.aop;

import com.rich.client.innerService.InnerUserService;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.model.annotation.AuthCheck;
import com.rich.model.entity.User;
import com.rich.model.enums.UserRoleEnum;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 权限校验的切面类
 *
 * @author DuRuiChi
 * @create 2025/8/5
 **/
@Aspect
@Component
public class AuthInterceptor {

    /**
     * 执行权限校验的切面方法
     *
     * @param joinPoint 连接点对象，用于获取被拦截方法的信息
     * @param authCheck 注解对象，包含需要的角色信息
     * @return 被拦截方法的执行结果
     * @throws Throwable 可能抛出的异常
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 从注解获取必须的角色值
        String mustRole = authCheck.mustRole();

        // 获取当前HTTP请求对象
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        // 从会话中获取当前登录用户
        User loginUser = InnerUserService.getLoginUser(request);

        // 将角色字符串转换为枚举类型
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);

        // 如果不需要特定权限，直接放行
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }

        // 获取当前用户的角色枚举
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());

        // 用户角色无效时拒绝访问
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 管理员权限校验：当需要管理员权限但用户不是管理员时
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 通过
        return joinPoint.proceed();
    }
}
