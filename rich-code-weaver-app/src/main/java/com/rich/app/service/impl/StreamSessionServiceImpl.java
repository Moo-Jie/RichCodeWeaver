package com.rich.app.service.impl;

import cn.hutool.core.util.StrUtil;
import com.rich.app.model.StreamEvent;
import com.rich.app.model.StreamSession;
import com.rich.app.service.StreamSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SSE 流式会话管理服务实现
 *
 * @author DuRuiChi
 * @create 2025/8/27
 **/
@Slf4j
@Service
public class StreamSessionServiceImpl implements StreamSessionService {

    private static final int SESSION_EXPIRE_MINUTES = 30;
    private static final int MAX_EVENTS_PER_SESSION = 10000;

    private final Map<String, StreamSession> sessionMap = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> eventIdCounters = new ConcurrentHashMap<>();

    @Override
    public String createSession(Long appId, Long userId) {
        String sessionId = generateSessionId(appId, userId);
        return createSession(sessionId, appId, userId);
    }

    @Override
    public String createSession(String sessionId, Long appId, Long userId) {
        StreamSession session = StreamSession.builder()
                .sessionId(sessionId)
                .appId(appId)
                .userId(userId)
                .createTime(LocalDateTime.now())
                .lastAccessTime(LocalDateTime.now())
                .eventQueue(new ConcurrentLinkedQueue<>())
                .completed(false)
                .hasError(false)
                .eventIdCounter(0L)
                .build();

        sessionMap.put(sessionId, session);
        eventIdCounters.put(sessionId, new AtomicLong(0));
        
        log.info("创建流式会话: sessionId={}, appId={}, userId={}", sessionId, appId, userId);
        return sessionId;
    }

    @Override
    public StreamSession getSession(String sessionId) {
        return sessionMap.get(sessionId);
    }

    @Override
    public void addEvent(String sessionId, StreamEvent event) {
        StreamSession session = sessionMap.get(sessionId);
        if (session == null) {
            log.warn("会话不存在: sessionId={}", sessionId);
            return;
        }

        ConcurrentLinkedQueue<StreamEvent> queue = session.getEventQueue();
        
        if (queue.size() >= MAX_EVENTS_PER_SESSION) {
            queue.poll();
        }
        
        queue.offer(event);
        session.setLastAccessTime(LocalDateTime.now());
        
        log.debug("添加事件到会话: sessionId={}, eventId={}, eventType={}", 
                sessionId, event.getEventId(), event.getEventType());
    }

    @Override
    public List<StreamEvent> getEventsAfter(String sessionId, String lastEventId) {
        StreamSession session = sessionMap.get(sessionId);
        if (session == null) {
            log.warn("会话不存在: sessionId={}", sessionId);
            return new ArrayList<>();
        }

        session.setLastAccessTime(LocalDateTime.now());
        
        List<StreamEvent> events = new ArrayList<>();
        ConcurrentLinkedQueue<StreamEvent> queue = session.getEventQueue();
        
        if (StrUtil.isEmpty(lastEventId)) {
            events.addAll(queue);
        } else {
            boolean found = false;
            for (StreamEvent event : queue) {
                if (found) {
                    events.add(event);
                } else if (event.getEventId().equals(lastEventId)) {
                    found = true;
                }
            }
        }
        
        if (events.isEmpty()) {
            log.debug("获取重连事件: sessionId={}, lastEventId={}, eventsCount=0", 
                    sessionId, lastEventId);
        } else {
            log.info("获取重连事件: sessionId={}, lastEventId={}, eventsCount={}", 
                    sessionId, lastEventId, events.size());
        }
        return events;
    }

    @Override
    public void markCompleted(String sessionId) {
        StreamSession session = sessionMap.get(sessionId);
        if (session != null) {
            session.setCompleted(true);
            session.setLastAccessTime(LocalDateTime.now());
            log.info("标记会话完成: sessionId={}", sessionId);
        }
    }

    @Override
    public void markError(String sessionId, String errorMessage) {
        StreamSession session = sessionMap.get(sessionId);
        if (session != null) {
            session.setHasError(true);
            session.setErrorMessage(errorMessage);
            session.setLastAccessTime(LocalDateTime.now());
            log.error("标记会话错误: sessionId={}, error={}", sessionId, errorMessage);
        }
    }

    @Override
    public void updateLastAccessTime(String sessionId) {
        StreamSession session = sessionMap.get(sessionId);
        if (session != null) {
            session.setLastAccessTime(LocalDateTime.now());
        }
    }

    @Override
    @Scheduled(fixedRate = 300000)
    public void cleanExpiredSessions() {
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(SESSION_EXPIRE_MINUTES);
        
        List<String> expiredSessions = new ArrayList<>();
        sessionMap.forEach((sessionId, session) -> {
            if (session.getLastAccessTime().isBefore(expireTime)) {
                expiredSessions.add(sessionId);
            }
        });
        
        expiredSessions.forEach(sessionId -> {
            sessionMap.remove(sessionId);
            eventIdCounters.remove(sessionId);
            log.info("清理过期会话: sessionId={}", sessionId);
        });
        
        if (!expiredSessions.isEmpty()) {
            log.info("清理了 {} 个过期会话", expiredSessions.size());
        }
    }

    @Override
    public String generateEventId(String sessionId) {
        AtomicLong counter = eventIdCounters.get(sessionId);
        if (counter == null) {
            counter = new AtomicLong(0);
            eventIdCounters.put(sessionId, counter);
        }
        return sessionId + "-" + counter.incrementAndGet();
    }

    private String generateSessionId(Long appId, Long userId) {
        return String.format("session-%d-%d-%d", appId, userId, System.currentTimeMillis());
    }
}
