package com.rich.richcodeweaver.aop;

import com.rich.richcodeweaver.annotation.RateLimit;
import com.rich.richcodeweaver.exception.BusinessException;
import com.rich.richcodeweaver.exception.ErrorCode;
import com.rich.richcodeweaver.model.entity.User;
import com.rich.richcodeweaver.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 限制接口的请求速率的切面类
 *
 * @author DuRuiChi
 * @create 2025/8/5
 **/
@Aspect
@Component
@Slf4j
public class RateLimitInterceptor {
    /**
     * 获取分布式限流器
     */
    @Resource
    private RedissonClient redissonClient;

    /**
     * 获取当前登录用户的信息
     */
    @Resource
    private UserService userService;

    /**
     * 默认的限流器过期时间（小时）
     */
    private static final long DEFAULT_EXPIRE_HOURS = 1L;

    /**
     * 获取令牌的最大等待时间（毫秒）
     */
    private static final long ACQUIRE_TIMEOUT_MS = 100L;

    /**
     * 限制接口的请求速率的切面方法，防止系统过载
     *
     * @param joinPoint 连接点，用于获取方法等信息
     * @param rateLimit 限流注解，用于获取限流配置
     * @return void
     * @author DuRuiChi
     * @create 2025/9/24
     **/
    @Before("@annotation(rateLimit)")
    public void doRateLimit(JoinPoint joinPoint, RateLimit rateLimit) {
        // 参数校验
        if (rateLimit == null) {
            log.warn("RateLimit 参数为空，跳过限流");
            return;
        }

        // 验证限流参数有效性
        if (!validateRateLimitParams(rateLimit)) {
            log.warn("RateLimit 参数无效，跳过限流： {}", joinPoint.getSignature().getName());
            return;
        }

        try {
            // 分限流类型拼接当前接口的限流 key
            String key = splicingCurrentLimitingKey(joinPoint, rateLimit);

            // 如果key为空或未知，使用默认key进行限流
            if (key == null || key.trim().isEmpty()) {
                key = generateFallbackKey(joinPoint);
                log.warn("使用降级限流 key: {}", key);
            }

            // 针对 key 执行限流
            executeRateLimiting(key, rateLimit, joinPoint);
        } catch (BusinessException e) {
            // 已知的业务异常直接抛出
            throw e;
        } catch (Exception e) {
            // 其他异常
            log.error("限流失败，跳过限流: {}, error: {}", joinPoint.getSignature().getName(), e.getMessage(), e);
        }
    }

    /**
     * 执行限流逻辑
     *
     * @param key       限流key
     * @param rateLimit 限流注解配置
     * @param joinPoint 连接点
     */
    private void executeRateLimiting(String key, RateLimit rateLimit, JoinPoint joinPoint) {
        // 获取 Redission 分布式限流器
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);

        try {
            // 设置限流器的过期时间
            boolean expireSet = rateLimiter.expire(Duration.ofHours(DEFAULT_EXPIRE_HOURS));
            if (!expireSet) {
                log.warn("未设置速率限制器的过期时间：{}", key);
            }

            // 设置限流规则: 作用范围,窗口时间内的最大请求速率, 窗口时间, 窗口时间单位
            boolean rateSet = rateLimiter.trySetRate(RateType.OVERALL, rateLimit.rate(), rateLimit.window(), RateIntervalUnit.SECONDS);
            if (!rateSet) {
                log.warn("未设置速率限制器的速率或窗口时间，限流key: {}", key);
            }

            // 尝试获取一个令牌，设置超时时间避免长时间阻塞
            boolean available = rateLimiter.tryAcquire(1, ACQUIRE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!available) {
                log.warn("接口 {} 被限流了，限流key: {}", joinPoint.getSignature().getName(), key);
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "接口访问过于频繁，请稍后再试");
            }

        } catch (Exception e) {
            // 处理Redisson操作异常
            if (e instanceof BusinessException) {
                throw (BusinessException) e;
            }
            log.error("限流操作失败，使用降级限流key: {}, 错误: {}", key, e.getMessage(), e);
        }
    }

    /**
     * 验证限流参数的有效性
     *
     * @param rateLimit 限流注解
     * @return 参数是否有效
     */
    private boolean validateRateLimitParams(RateLimit rateLimit) {
        if (rateLimit.rate() <= 0) {
            return false;
        }
        if (rateLimit.window() <= 0) {
            return false;
        }
        return rateLimit.type() != null;
    }

    /**
     * 生成降级用的限流key
     *
     * @param joinPoint 连接点
     * @return 降级key
     */
    private String generateFallbackKey(JoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        return "rate_limit:fallback:" + method.getDeclaringClass().getName() + ":" + method.getName();
    }

    /**
     * 分限流类型拼接当前接口的限流 key
     *
     * @param joinPoint 连接点，用于获取方法等信息
     * @param rateLimit 限流注解，用于获取限流配置
     * @return java.lang.String
     * @author DuRuiChi
     * @create 2025/9/24
     **/
    private String splicingCurrentLimitingKey(JoinPoint joinPoint, RateLimit rateLimit) {
        // 拼接限流 key
        StringBuilder key = new StringBuilder("rate_limit:");

        try {
            // 分限流类型拼接
            switch (rateLimit.type()) {
                // 针对用户限流
                case USER:
                    User loginUser = getCurrentLoginUser();
                    if (loginUser != null && loginUser.getId() != null) {
                        key.append("user:").append(loginUser.getId());
                    } else {
                        // 如果没有获取到登录用户，则降级为使用请求 IP 限流
                        log.warn("无法获取登录用户，使用请求 IP 限流");
                        key.append("ip:").append(getClientIP());
                    }
                    break;

                // 针对请求 IP 限流
                case IP:
                    key.append("ip:").append(getClientIP());
                    break;

                // 针对 API 限流
                case API:
                    Method method = getTargetMethod(joinPoint);
                    if (method != null) {
                        // 拼接类名 + 方法名保证唯一性
                        key.append("api:").append(method.getDeclaringClass().getName()).append(":").append(method.getName());
                    } else {
                        // 方法获取失败时的降级处理
                        log.warn("无法获取目标方法，使用未知 API 限流");
                        key.append(generateFallbackKey(joinPoint));
                    }
                    break;
                default:
                    log.error("不支持的限流类型: {}", rateLimit.type());
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "不支持的限流类型");
            }
        } catch (Exception e) {
            log.error("限流 key 拼接失败", e);
            return generateFallbackKey(joinPoint);
        }

        return key.toString();
    }

    /**
     * 获取当前登录用户
     *
     * @return 登录用户对象，获取失败返回null
     */
    private User getCurrentLoginUser() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return userService.getLoginUser(request);
            }
        } catch (Exception e) {
            log.warn("获取当前登录用户失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取目标方法
     *
     * @param joinPoint 连接点
     * @return 方法对象，获取失败返回null
     */
    private Method getTargetMethod(JoinPoint joinPoint) {
        try {
            return ((MethodSignature) joinPoint.getSignature()).getMethod();
        } catch (Exception e) {
            log.warn("方法获取失败： {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前请求的 IP 地址
     *
     * @return java.lang.String
     * @author DuRuiChi
     * @create 2025/9/24
     **/
    private String getClientIP() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return "unknown";
            }

            HttpServletRequest request = attributes.getRequest();
            String ip = null;

            // 按优先级尝试不同的IP获取方式
            String[] headerNames = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};

            for (String header : headerNames) {
                ip = request.getHeader(header);
                if (isValidIP(ip)) {
                    break;
                }
            }

            // 如果头信息中未获取到有效IP，使用远程地址
            if (!isValidIP(ip)) {
                ip = request.getRemoteAddr();
            }

            // 处理多级代理的情况，取第一个有效IP
            if (ip != null && ip.contains(",")) {
                String[] ips = ip.split(",");
                for (String singleIp : ips) {
                    String trimmedIp = singleIp.trim();
                    if (isValidIP(trimmedIp)) {
                        ip = trimmedIp;
                        break;
                    }
                }
            }

            return isValidIP(ip) ? ip : "unknown";

        } catch (Exception e) {
            log.warn("Failed to get client IP: {}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * 验证IP地址是否有效
     *
     * @param ip IP地址
     * @return 是否有效
     */
    private boolean isValidIP(String ip) {
        return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip) && !"0:0:0:0:0:0:0:1".equals(ip) && !"127.0.0.1".equals(ip);
    }
}