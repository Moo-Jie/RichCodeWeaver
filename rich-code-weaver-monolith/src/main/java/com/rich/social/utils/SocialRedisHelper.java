package com.rich.social.utils;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 社区模块 Redis 工具类
 * 提供分布式锁等功能，防止社交操作的并发问题
 * 使用 StringRedisTemplate 的 SETNX 命令实现分布式锁，默认过期时间5秒防止死锁
 *
 * @author DuRuiChi
 * @create 2026-03-25
 */
@Component
public class SocialRedisHelper {

    /**
     * 锁默认过期时间（秒），防止死锁
     */
    private static final long LOCK_EXPIRE_SECONDS = 5;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 生成点赞操作锁key
     * 格式: social:lock:like:{appId}:{userId}
     *
     * @param appId  产物id
     * @param userId 用户id
     * @return 锁key
     */
    public static String likeLockKey(Long appId, Long userId) {
        return "social:lock:like:" + appId + ":" + userId;
    }

    /**
     * 生成收藏操作锁key
     * 格式: social:lock:favorite:{appId}:{userId}
     *
     * @param appId  产物id
     * @param userId 用户id
     * @return 锁key
     */
    public static String favoriteLockKey(Long appId, Long userId) {
        return "social:lock:favorite:" + appId + ":" + userId;
    }

    /**
     * 生成社区帖子点赞操作锁key
     * 格式: social:lock:communityPostLike:{postId}:{userId}
     *
     * @param postId  帖子id
     * @param userId  用户id
     * @return 锁key
     */
    public static String communityPostLikeLockKey(Long postId, Long userId) {
        return "social:lock:communityPostLike:" + postId + ":" + userId;
    }

    /**
     * 生成社区回复点赞操作锁key
     * 格式: social:lock:communityReplyLike:{replyId}:{userId}
     *
     * @param replyId 回复id
     * @param userId  用户id
     * @return 锁key
     */
    public static String communityReplyLikeLockKey(Long replyId, Long userId) {
        return "social:lock:communityReplyLike:" + replyId + ":" + userId;
    }

    /**
     * 生成评论点赞操作锁key
     * 格式: social:lock:commentLike:{commentId}:{userId}
     *
     * @param commentId 评论id
     * @param userId    用户id
     * @return 锁key
     */
    public static String commentLikeLockKey(Long commentId, Long userId) {
        return "social:lock:commentLike:" + commentId + ":" + userId;
    }

    /**
     * 生成热点统计初始化锁key
     * 格式: social:lock:hotStatInit:{appId}
     *
     * @param appId 产物id
     * @return 锁key
     */
    public static String hotStatInitLockKey(Long appId) {
        return "social:lock:hotStatInit:" + appId;
    }

    /**
     * 获取分布式锁（非阻塞）
     * 使用 Redis SETNX 命令，若 key 不存在则设置成功并返回 true
     * 自动设置过期时间为 5 秒，防止死锁
     *
     * @param lockKey 锁的key
     * @return true=获取锁成功, false=锁已被占用
     */
    public boolean tryLock(String lockKey) {
        Boolean result = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 释放分布式锁
     * 直接删除 Redis key，释放锁资源
     *
     * @param lockKey 锁的key
     */
    public void releaseLock(String lockKey) {
        stringRedisTemplate.delete(lockKey);
    }
}
