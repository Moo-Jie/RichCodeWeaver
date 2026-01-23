package com.rich.richcodeweaver.service.codegen;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 代码生成任务的 Redis 持久化（用于刷新/断线重连的断点续传）
 */
@Repository
@RequiredArgsConstructor
public class CodeGenTaskRepository {

    private static final String TASK_META_KEY_PREFIX = "rcw:codegen:task:";
    private static final String TASK_CHUNKS_SUFFIX = ":chunks";
    private static final String ACTIVE_TASK_PREFIX = "rcw:codegen:active:";

    /**
     * 任务与断点信息保存时间（避免 Redis 无限制增长）
     */
    private static final Duration TASK_TTL = Duration.ofDays(3);

    private final StringRedisTemplate stringRedisTemplate;

    public String taskMetaKey(String taskId) {
        return TASK_META_KEY_PREFIX + taskId;
    }

    public String taskChunksKey(String taskId) {
        return TASK_META_KEY_PREFIX + taskId + TASK_CHUNKS_SUFFIX;
    }

    public String activeTaskKey(long userId, long appId) {
        return ACTIVE_TASK_PREFIX + userId + ":" + appId;
    }

    public void initTask(String taskId, long userId, long appId, String codeGenType) {
        String metaKey = taskMetaKey(taskId);
        stringRedisTemplate.opsForHash().putAll(metaKey, Map.of(
                "taskId", taskId,
                "userId", String.valueOf(userId),
                "appId", String.valueOf(appId),
                "codeGenType", codeGenType == null ? "" : codeGenType,
                "status", CodeGenTaskStatus.RUNNING.name(),
                "seq", "0"
        ));
        stringRedisTemplate.expire(metaKey, TASK_TTL);
        stringRedisTemplate.expire(taskChunksKey(taskId), TASK_TTL);
        stringRedisTemplate.opsForValue().set(activeTaskKey(userId, appId), taskId, TASK_TTL);
    }

    public void appendChunk(String taskId, long seq, String chunk) {
        String metaKey = taskMetaKey(taskId);
        stringRedisTemplate.opsForList().rightPush(taskChunksKey(taskId), chunk == null ? "" : chunk);
        stringRedisTemplate.opsForHash().put(metaKey, "seq", String.valueOf(seq));
        // 续命
        stringRedisTemplate.expire(metaKey, TASK_TTL);
        stringRedisTemplate.expire(taskChunksKey(taskId), TASK_TTL);
    }

    public List<String> readChunksFromSeq(String taskId, long fromSeqInclusive) {
        if (fromSeqInclusive <= 1) {
            return stringRedisTemplate.opsForList().range(taskChunksKey(taskId), 0, -1);
        }
        long startIndex = fromSeqInclusive - 1; // seq 从 1 开始，对应 list index 从 0 开始
        return stringRedisTemplate.opsForList().range(taskChunksKey(taskId), startIndex, -1);
    }

    public long getLastSeq(String taskId) {
        Object val = stringRedisTemplate.opsForHash().get(taskMetaKey(taskId), "seq");
        if (val == null) return 0L;
        try {
            return Long.parseLong(String.valueOf(val));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public void markSucceeded(String taskId, long userId, long appId) {
        stringRedisTemplate.opsForHash().put(taskMetaKey(taskId), "status", CodeGenTaskStatus.SUCCEEDED.name());
        stringRedisTemplate.delete(activeTaskKey(userId, appId));
    }

    public void markFailed(String taskId, long userId, long appId, String errorMessage) {
        stringRedisTemplate.opsForHash().putAll(taskMetaKey(taskId), Map.of(
                "status", CodeGenTaskStatus.FAILED.name(),
                "error", errorMessage == null ? "" : errorMessage
        ));
        stringRedisTemplate.delete(activeTaskKey(userId, appId));
    }

    public void markCancelled(String taskId, long userId, long appId) {
        stringRedisTemplate.opsForHash().put(taskMetaKey(taskId), "status", CodeGenTaskStatus.CANCELLED.name());
        stringRedisTemplate.delete(activeTaskKey(userId, appId));
    }

    public Optional<String> findActiveTaskId(long userId, long appId) {
        String taskId = stringRedisTemplate.opsForValue().get(activeTaskKey(userId, appId));
        return Optional.ofNullable(taskId);
    }

    public Optional<String> getStatus(String taskId) {
        Object val = stringRedisTemplate.opsForHash().get(taskMetaKey(taskId), "status");
        return Optional.ofNullable(val).map(String::valueOf);
    }
}


