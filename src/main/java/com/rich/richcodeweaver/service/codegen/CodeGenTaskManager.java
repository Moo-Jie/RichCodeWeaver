package com.rich.richcodeweaver.service.codegen;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rich.richcodeweaver.websocket.dto.CodeGenWsServerMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.Disposable;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 代码生成任务管理：后台持续生成 + Redis 持久化 + WebSocket 订阅/断点续传
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeGenTaskManager {

    private final CodeGenStreamService codeGenStreamService;
    private final CodeGenTaskRepository taskRepository;
    private final ObjectMapper objectMapper;

    /**
     * taskId -> runtime
     */
    private final Map<String, TaskRuntime> runtimes = new ConcurrentHashMap<>();

    /**
     * 启动新任务（若已有进行中的任务，可直接返回已有 taskId）
     */
    public String startOrGetActive(long userId, long appId, String message, boolean isAgent, WebSocketSession session) {
        String active = taskRepository.findActiveTaskId(userId, appId).orElse(null);
        if (active != null) {
            TaskRuntime rt = runtimes.get(active);
            if (rt != null && rt.status == CodeGenTaskStatus.RUNNING) {
                subscribe(active, session);
                return active;
            }
        }
        return startNew(userId, appId, message, isAgent, session);
    }

    private String startNew(long userId, long appId, String message, boolean isAgent, WebSocketSession session) {
        String taskId = UUID.randomUUID().toString().replace("-", "");

        // 初始化 Redis 元信息（codeGenType 暂不强制写入；后续可补）
        taskRepository.initTask(taskId, userId, appId, "");

        TaskRuntime runtime = new TaskRuntime(taskId, userId, appId);
        runtimes.put(taskId, runtime);
        subscribe(taskId, session);

        log.info("CodeGen task started: taskId={}, userId={}, appId={}, isAgent={}", taskId, userId, appId, isAgent);

        // 立即发一个“首包”，让前端立刻可见并验证 WS 链路，同时也写入 Redis 以支持刷新续传
        {
            long seq = runtime.nextSeq();
            String first = "开始生成...\n";
            taskRepository.appendChunk(taskId, seq, first);
            broadcast(taskId, CodeGenWsServerMessage.builder()
                    .type("chunk")
                    .taskId(taskId)
                    .appId(String.valueOf(appId))
                    .seq(seq)
                    .data(first)
                    .build());
        }

        // 后台开始生成（与连接解耦）
        // bufferTimeout：降低 ws 消息数量与 Redis list 长度，提升稳定性
        Disposable disposable = codeGenStreamService
                .createUserFacingStream(appId, userId, message, isAgent)
                .bufferTimeout(50, Duration.ofMillis(200))
                .map(list -> String.join("", list))
                .filter(s -> s != null && !s.isEmpty())
                .subscribe(
                        chunk -> {
                            long seq = runtime.nextSeq();
                            taskRepository.appendChunk(taskId, seq, chunk);
                            log.debug("CodeGen task chunk: taskId={}, seq={}, size={}", taskId, seq, chunk.length());
                            broadcast(taskId, CodeGenWsServerMessage.builder()
                                    .type("chunk")
                                    .taskId(taskId)
                                    .appId(String.valueOf(appId))
                                    .seq(seq)
                                    .data(chunk)
                                    .build());
                        },
                        error -> {
                            runtime.status = CodeGenTaskStatus.FAILED;
                            log.error("CodeGen task failed: taskId={}, userId={}, appId={}, err={}", taskId, userId, appId, error.getMessage(), error);
                            taskRepository.markFailed(taskId, userId, appId, error.getMessage());
                            broadcast(taskId, CodeGenWsServerMessage.builder()
                                    .type("error")
                                    .taskId(taskId)
                                    .appId(String.valueOf(appId))
                                    .message(error.getMessage())
                                    .build());
                            cleanup(taskId);
                        },
                        () -> {
                            runtime.status = CodeGenTaskStatus.SUCCEEDED;
                            log.info("CodeGen task succeeded: taskId={}, userId={}, appId={}, lastSeq={}", taskId, userId, appId, runtime.seq);
                            taskRepository.markSucceeded(taskId, userId, appId);
                            broadcast(taskId, CodeGenWsServerMessage.builder()
                                    .type("end")
                                    .taskId(taskId)
                                    .appId(String.valueOf(appId))
                                    .seq(runtime.seq)
                                    .build());
                            cleanup(taskId);
                        }
                );

        runtime.disposable = disposable;
        return taskId;
    }

    public void subscribe(String taskId, WebSocketSession session) {
        TaskRuntime rt = runtimes.get(taskId);
        if (rt == null) {
            // 允许订阅“只存在于 Redis 的已完成任务”（用于回放），此时不维护 subscribers
            return;
        }
        rt.subscribers.add(session);
    }

    public void unsubscribeAll(WebSocketSession session) {
        runtimes.values().forEach(rt -> rt.subscribers.remove(session));
    }

    /**
     * 断点续传：从指定 seq（包含）回放 Redis 中的 chunk，然后订阅后续实时输出（若任务仍在运行）
     */
    public void resume(String taskId, long fromSeqInclusive, WebSocketSession session, long appId) {
        // 先回放
        long seq = fromSeqInclusive;
        for (String chunk : taskRepository.readChunksFromSeq(taskId, fromSeqInclusive)) {
            safeSend(session, CodeGenWsServerMessage.builder()
                    .type("chunk")
                    .taskId(taskId)
                    .appId(String.valueOf(appId))
                    .seq(seq++)
                    .data(chunk)
                    .build());
        }

        // 再订阅实时（如果还在跑）
        TaskRuntime rt = runtimes.get(taskId);
        if (rt != null && rt.status == CodeGenTaskStatus.RUNNING) {
            rt.subscribers.add(session);
        } else {
            // 任务已结束，发一个 end/error 给前端，便于 UI 收尾
            String status = taskRepository.getStatus(taskId).orElse("");
            if (CodeGenTaskStatus.SUCCEEDED.name().equals(status)) {
                safeSend(session, CodeGenWsServerMessage.builder()
                        .type("end")
                        .taskId(taskId)
                        .appId(String.valueOf(appId))
                        .seq(taskRepository.getLastSeq(taskId))
                        .build());
            } else if (CodeGenTaskStatus.FAILED.name().equals(status)) {
                safeSend(session, CodeGenWsServerMessage.builder()
                        .type("error")
                        .taskId(taskId)
                        .appId(String.valueOf(appId))
                        .message("任务已失败（可刷新历史对话查看详情）")
                        .build());
            }
        }
    }

    public void cancel(String taskId) {
        TaskRuntime rt = runtimes.get(taskId);
        if (rt == null) return;
        rt.status = CodeGenTaskStatus.CANCELLED;
        if (rt.disposable != null) {
            rt.disposable.dispose();
        }
        taskRepository.markCancelled(taskId, rt.userId, rt.appId);
        broadcast(taskId, CodeGenWsServerMessage.builder()
                .type("end")
                .taskId(taskId)
                .appId(String.valueOf(rt.appId))
                .seq(rt.seq)
                .message("已取消")
                .build());
        cleanup(taskId);
    }

    public void safeSend(WebSocketSession session, CodeGenWsServerMessage msg) {
        if (session == null || !session.isOpen()) return;
        try {
            String json = objectMapper.writeValueAsString(msg);
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.warn("WebSocket send failed: {}", e.getMessage());
        }
    }

    private void broadcast(String taskId, CodeGenWsServerMessage msg) {
        TaskRuntime rt = runtimes.get(taskId);
        if (rt == null) return;
        for (WebSocketSession s : rt.subscribers) {
            safeSend(s, msg);
        }
    }

    private void cleanup(String taskId) {
        TaskRuntime rt = runtimes.remove(taskId);
        if (rt != null) {
            rt.subscribers.clear();
        }
    }

    private static class TaskRuntime {
        final long userId;
        final long appId;
        volatile long seq = 0;
        volatile CodeGenTaskStatus status = CodeGenTaskStatus.RUNNING;
        volatile Disposable disposable;
        final Set<WebSocketSession> subscribers = ConcurrentHashMap.newKeySet();

        TaskRuntime(String taskId, long userId, long appId) {
            this.userId = userId;
            this.appId = appId;
        }

        long nextSeq() {
            return ++seq;
        }
    }
}


