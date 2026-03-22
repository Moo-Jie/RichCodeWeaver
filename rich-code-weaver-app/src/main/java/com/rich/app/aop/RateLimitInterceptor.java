package com.rich.app.aop;

import com.rich.client.innerService.InnerUserService;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.model.annotation.RateLimit;
import com.rich.model.entity.User;
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
 * 基于Redisson实现分布式限流，支持按用户、IP、API三种维度进行限流
 *
 * @author DuRuiChi
 * @since 2026-03-08
 */
@Aspect
@Component
@Slf4j
public class RateLimitInterceptor {
    /**
     * 默认的限流器过期时间（小时）
     */
    private static final long DEFAULT_EXPIRE_HOURS = 1L;
    /**
     * 获取令牌的最大等待时间（毫秒）
     */
    private static final long ACQUIRE_TIMEOUT_MS = 100L;
    /**
     * 获取分布式限流器
     */
    @Resource
    private RedissonClient redissonClient;

    /**
     * 限制接口的请求速率的切面方法，防止系统过载
     *
     * @param joinPoint 连接点，用于获取方法等信息
     * @param rateLimit 限流注解，用于获取限流配置
     * @author DuRuiChi
     */
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
     * @author DuRuiChi
     */
    private void executeRateLimiting(String key, RateLimit rateLimit, JoinPoint joinPoint) {
        // 获取 Redisson 分布式限流器实例
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);

        try {
            // 设置限流器的过期时间（防止Redis中的key永久存在）
            boolean expireSet = rateLimiter.expire(Duration.ofHours(DEFAULT_EXPIRE_HOURS));
            if (!expireSet) {
                log.warn("限流器过期时间设置失败: {}", key);
            }

            // 设置限流规则：作用范围(OVERALL)、窗口时间内的最大请求数、窗口时间、时间单位(秒)
            boolean rateSet = rateLimiter.trySetRate(
                    RateType.OVERALL, 
                    rateLimit.rate(), 
                    rateLimit.window(), 
                    RateIntervalUnit.SECONDS);
            if (!rateSet) {
                log.warn("限流规则设置失败，限流key: {}", key);
            }

            // 尝试获取一个令牌，设置超时时间避免长时间阻塞
            boolean available = rateLimiter.tryAcquire(1, ACQUIRE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!available) {
                // 获取令牌失败，说明请求过于频繁，触发限流
                log.warn("接口 {} 被限流，限流key: {}", joinPoint.getSignature().getName(), key);
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "接口访问过于频繁，请稍后再试");
            }

        } catch (Exception e) {
            // 处理 Redisson 操作异常
            if (e instanceof BusinessException) {
                // 业务异常直接向上抛出
                throw (BusinessException) e;
            }
            // 其他异常记录日志，但不阻断请求（限流降级）
            log.error("限流操作失败，限流key: {}, 错误: {}", key, e.getMessage(), e);
        }
    }

    /**
     * 验证限流参数的有效性
     *
     * @param rateLimit 限流注解
     * @return 参数是否有效
     * @author DuRuiChi
     */
    private boolean validateRateLimitParams(RateLimit rateLimit) {
        // 验证限流速率（必须大于0）
        if (rateLimit.rate() <= 0) {
            return false;
        }
        // 验证窗口时间（必须大于0）
        if (rateLimit.window() <= 0) {
            return false;
        }
        // 验证限流类型（不能为null）
        return rateLimit.type() != null;
    }

    /**
     * 生成降级用的限流key
     *
     * @param joinPoint 连接点
     * @return 降级key
     * @author DuRuiChi
     */
    private String generateFallbackKey(JoinPoint joinPoint) {
        // 获取目标方法
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        // 生成降级限流key：使用类名+方法名保证唯一性
        return "rate_limit:fallback:" + method.getDeclaringClass().getName() + ":" + method.getName();
    }

    /**
     * 分限流类型拼接当前接口的限流 key
     *
     * @param joinPoint 连接点，用于获取方法等信息
     * @param rateLimit 限流注解，用于获取限流配置
     * @return 限流key字符串
     * @author DuRuiChi
     */
    private String splicingCurrentLimitingKey(JoinPoint joinPoint, RateLimit rateLimit) {
        // 初始化限流key的前缀
        StringBuilder key = new StringBuilder("rate_limit:");

        try {
            // 根据限流类型拼接不同的key
            switch (rateLimit.type()) {
                // 针对用户限流：基于用户ID
                case USER:
                    User loginUser = getCurrentLoginUser();
                    if (loginUser != null && loginUser.getId() != null) {
                        // 使用用户ID作为限流维度
                        key.append("user:").append(loginUser.getId());
                    } else {
                        // 如果没有获取到登录用户，则降级为使用请求IP限流
                        log.warn("无法获取登录用户，降级为IP限流");
                        key.append("ip:").append(getClientIP());
                    }
                    break;

                // 针对请求IP限流：基于客户端IP地址
                case IP:
                    String clientIP = getClientIP();
                    key.append("ip:").append(clientIP);
                    break;

                // 针对API限流：基于接口方法
                case API:
                    Method method = getTargetMethod(joinPoint);
                    if (method != null) {
                        // 拼接类名 + 方法名保证接口的唯一性
                        key.append("api:")
                           .append(method.getDeclaringClass().getName())
                           .append(":")
                           .append(method.getName());
                    } else {
                        // 方法获取失败时的降级处理
                        log.warn("无法获取目标方法，使用降级key");
                        return generateFallbackKey(joinPoint);
                    }
                    break;
                default:
                    // 不支持的限流类型
                    log.error("不支持的限流类型: {}", rateLimit.type());
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "不支持的限流类型");
            }
        } catch (Exception e) {
            // key拼接失败时使用降级key
            log.error("限流key拼接失败，使用降级key", e);
            return generateFallbackKey(joinPoint);
        }

        return key.toString();
    }

    /**
     * 获取当前登录用户
     *
     * @return 登录用户对象，获取失败返回null
     * @author DuRuiChi
     */
    private User getCurrentLoginUser() {
        try {
            // 获取当前请求的属性
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                // 从请求属性中获取HttpServletRequest
                HttpServletRequest request = attributes.getRequest();
                // 通过用户服务获取当前登录用户
                return InnerUserService.getLoginUser(request);
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
     * @author DuRuiChi
     */
    private Method getTargetMethod(JoinPoint joinPoint) {
        try {
            // 从连接点的签名中获取目标方法
            return ((MethodSignature) joinPoint.getSignature()).getMethod();
        } catch (Exception e) {
            log.warn("获取目标方法失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前请求的 IP 地址
     * 支持从多种代理头中获取真实IP
     *
     * @return 客户端IP地址
     * @author DuRuiChi
     */
    private String getClientIP() {
        try {
            // 获取当前请求的属性
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return "unknown";
            }

            HttpServletRequest request = attributes.getRequest();
            String ip = null;

            // 按优先级尝试从不同的HTTP头中获取真实IP（支持代理场景）
            String[] headerNames = {
                "X-Forwarded-For",      // 标准的代理头
                "X-Real-IP",            // Nginx代理头
                "Proxy-Client-IP",      // Apache代理头
                "WL-Proxy-Client-IP",   // WebLogic代理头
                "HTTP_CLIENT_IP",       // 通用代理头
                "HTTP_X_FORWARDED_FOR"  // 通用代理头
            };

            // 遍历所有可能的头信息，找到第一个有效的IP
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

            // 处理多级代理的情况（IP列表以逗号分隔），取第一个有效IP
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
            log.warn("获取客户端IP失败: {}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * 验证IP地址是否有效
     *
     * @param ip IP地址
     * @return 是否有效
     * @author DuRuiChi
     */
    private boolean isValidIP(String ip) {
        // 验证IP是否有效：排除null、空字符串、"unknown"、IPv6本地地址、IPv4本地地址
        return ip != null 
                && !ip.isEmpty() 
                && !"unknown".equalsIgnoreCase(ip) 
                && !"0:0:0:0:0:0:0:1".equals(ip)  // IPv6本地地址
                && !"127.0.0.1".equals(ip);        // IPv4本地地址
    }
}