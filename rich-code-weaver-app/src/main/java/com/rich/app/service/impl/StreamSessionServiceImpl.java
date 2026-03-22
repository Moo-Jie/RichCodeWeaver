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
 * @create 2025/12/27
 **/
@Slf4j
@Service
public class StreamSessionServiceImpl implements StreamSessionService {

    private static final int SESSION_EXPIRE_MINUTES = 30;
    private static final int MAX_EVENTS_PER_SESSION = 10000;

    private final Map<String, StreamSession> sessionMap = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> eventIdCounters = new ConcurrentHashMap<>();

    /**
     * 创建流式会话
     * 根据产物ID和用户ID生成会话ID并创建会话
     *
     * @param appId  产物ID
     * @param userId 用户ID
     * @return String 会话ID
     * @author DuRuiChi
     */
    @Override
    public String createSession(Long appId, Long userId) {
        // 参数校验：确保产物ID和用户ID有效
        if (appId == null || appId <= 0 || userId == null || userId <= 0) {
            log.error("创建会话失败：参数无效 - appId={}, userId={}", appId, userId);
            throw new IllegalArgumentException("产物ID和用户ID不能为空或小于等于0");
        }
        
        // 生成唯一会话ID
        String sessionId = generateSessionId(appId, userId);
        return createSession(sessionId, appId, userId);
    }

    /**
     * 使用指定会话ID创建流式会话
     * 创建新的会话对象并初始化事件队列和计数器
     *
     * @param sessionId 会话ID
     * @param appId     产物ID
     * @param userId    用户ID
     * @return String 会话ID
     * @author DuRuiChi
     */
    @Override
    public String createSession(String sessionId, Long appId, Long userId) {
        // 参数校验：确保会话ID、产物ID和用户ID有效
        if (StrUtil.isBlank(sessionId)) {
            log.error("创建会话失败：会话ID为空");
            throw new IllegalArgumentException("会话ID不能为空");
        }
        if (appId == null || appId <= 0 || userId == null || userId <= 0) {
            log.error("创建会话失败：参数无效 - sessionId={}, appId={}, userId={}", sessionId, appId, userId);
            throw new IllegalArgumentException("产物ID和用户ID不能为空或小于等于0");
        }
        
        // 检查会话是否已存在，避免重复创建
        if (sessionMap.containsKey(sessionId)) {
            log.warn("会话已存在，返回现有会话ID: sessionId={}", sessionId);
            return sessionId;
        }
        
        // 获取当前时间（统一使用同一时间戳）
        LocalDateTime currentTime = LocalDateTime.now();
        
        // 构建流式会话对象
        StreamSession session = StreamSession.builder()
                .sessionId(sessionId)
                .appId(appId)
                .userId(userId)
                .createTime(currentTime)
                .lastAccessTime(currentTime)
                .eventQueue(new ConcurrentLinkedQueue<>())  // 初始化线程安全的事件队列
                .completed(false)  // 初始状态为未完成
                .hasError(false)   // 初始状态为无错误
                .eventIdCounter(0L)  // 事件ID计数器从0开始
                .build();

        // 将会话存入会话映射表（线程安全）
        sessionMap.put(sessionId, session);
        // 初始化事件ID计数器（原子操作，线程安全）
        eventIdCounters.put(sessionId, new AtomicLong(0));
        
        log.info("创建流式会话成功: sessionId={}, appId={}, userId={}", sessionId, appId, userId);
        return sessionId;
    }

    /**
     * 获取流式会话
     * 根据会话ID获取会话对象
     *
     * @param sessionId 会话ID
     * @return StreamSession 会话对象，不存在则返回 null
     * @author DuRuiChi
     */
    @Override
    public StreamSession getSession(String sessionId) {
        // 从会话映射表中获取会话对象
        return sessionMap.get(sessionId);
    }

    /**
     * 添加事件到会话
     * 将事件添加到指定会话的事件队列中，超过最大容量时移除最旧的事件
     *
     * @param sessionId 会话ID
     * @param event     流式事件
     * @author DuRuiChi
     */
    @Override
    public void addEvent(String sessionId, StreamEvent event) {
        // 参数校验：确保会话ID和事件对象有效
        if (StrUtil.isBlank(sessionId)) {
            log.warn("添加事件失败：会话ID为空");
            return;
        }
        if (event == null) {
            log.warn("添加事件失败：事件对象为空 - sessionId={}", sessionId);
            return;
        }
        
        // 获取会话对象
        StreamSession session = sessionMap.get(sessionId);
        if (session == null) {
            log.warn("会话不存在，无法添加事件: sessionId={}", sessionId);
            return;
        }

        // 获取会话的事件队列（线程安全队列）
        ConcurrentLinkedQueue<StreamEvent> eventQueue = session.getEventQueue();
        if (eventQueue == null) {
            log.error("事件队列为空，会话数据异常: sessionId={}", sessionId);
            return;
        }
        
        // 检查队列容量，超过最大限制时移除最旧的事件（FIFO策略，防止内存溢出）
        if (eventQueue.size() >= MAX_EVENTS_PER_SESSION) {
            StreamEvent removedEvent = eventQueue.poll();
            log.debug("事件队列已满（容量={}），移除最旧事件: eventId={}", 
                    MAX_EVENTS_PER_SESSION,
                    removedEvent != null ? removedEvent.getEventId() : "null");
        }
        
        // 将新事件添加到队列尾部（线程安全操作）
        boolean addSuccess = eventQueue.offer(event);
        if (!addSuccess) {
            log.error("事件添加失败: sessionId={}, eventId={}", sessionId, event.getEventId());
            return;
        }
        
        // 更新会话最后访问时间（保持会话活跃）
        session.setLastAccessTime(LocalDateTime.now());
        
        log.debug("添加事件到会话成功: sessionId={}, eventId={}, eventType={}, queueSize={}", 
                sessionId, event.getEventId(), event.getEventType(), eventQueue.size());
    }

    /**
     * 获取指定事件之后的所有事件
     * 用于SSE重连时获取断点之后的事件，支持断点续传
     *
     * @param sessionId   会话ID
     * @param lastEventId 最后接收到的事件ID，为空则返回所有事件
     * @return List<StreamEvent> 事件列表
     * @author DuRuiChi
     */
    @Override
    public List<StreamEvent> getEventsAfter(String sessionId, String lastEventId) {
        // 参数校验：确保会话ID有效
        if (StrUtil.isBlank(sessionId)) {
            log.warn("获取事件失败：会话ID为空");
            return new ArrayList<>();
        }
        
        // 获取会话对象
        StreamSession session = sessionMap.get(sessionId);
        if (session == null) {
            log.warn("会话不存在，无法获取事件: sessionId={}", sessionId);
            return new ArrayList<>();
        }

        // 更新会话最后访问时间（保持会话活跃，防止被清理）
        session.setLastAccessTime(LocalDateTime.now());
        
        // 获取事件队列
        ConcurrentLinkedQueue<StreamEvent> eventQueue = session.getEventQueue();
        if (eventQueue == null || eventQueue.isEmpty()) {
            // 队列为空或不存在，返回空列表
            return new ArrayList<>();
        }
        
        // 准备返回的事件列表
        List<StreamEvent> resultEvents = new ArrayList<>();
        
        // 如果没有指定lastEventId，返回所有事件（首次连接场景）
        if (StrUtil.isEmpty(lastEventId)) {
            resultEvents.addAll(eventQueue);
        } else {
            // 查找lastEventId之后的所有事件（断线重连场景）
            boolean foundLastEvent = false;
            for (StreamEvent event : eventQueue) {
                // 空值保护
                if (event == null || event.getEventId() == null) {
                    continue;
                }
                
                if (foundLastEvent) {
                    // 已找到lastEventId，添加后续所有事件
                    resultEvents.add(event);
                } else if (event.getEventId().equals(lastEventId)) {
                    // 找到lastEventId，从下一个事件开始添加
                    foundLastEvent = true;
                }
            }
            
            // 如果没有找到lastEventId，可能是事件已被清理，返回所有事件
            if (!foundLastEvent && !eventQueue.isEmpty()) {
                log.warn("未找到指定的lastEventId，返回所有事件: sessionId={}, lastEventId={}", 
                        sessionId, lastEventId);
                resultEvents.addAll(eventQueue);
            }
        }
        
        return resultEvents;
    }

    /**
     * 标记会话为已完成
     * 设置会话的完成状态并更新最后访问时间
     *
     * @param sessionId 会话ID
     * @author DuRuiChi
     */
    @Override
    public void markCompleted(String sessionId) {
        // 获取会话对象
        StreamSession session = sessionMap.get(sessionId);
        if (session != null) {
            // 标记会话为已完成状态
            session.setCompleted(true);
            // 更新最后访问时间
            session.setLastAccessTime(LocalDateTime.now());
            log.info("标记会话完成: sessionId={}", sessionId);
        } else {
            log.warn("会话不存在，无法标记完成: sessionId={}", sessionId);
        }
    }

    /**
     * 标记会话为错误状态
     * 设置会话的错误状态和错误消息
     *
     * @param sessionId    会话ID
     * @param errorMessage 错误消息
     * @author DuRuiChi
     */
    @Override
    public void markError(String sessionId, String errorMessage) {
        // 获取会话对象
        StreamSession session = sessionMap.get(sessionId);
        if (session != null) {
            // 标记会话为错误状态
            session.setHasError(true);
            // 记录错误消息
            session.setErrorMessage(errorMessage);
            // 更新最后访问时间
            session.setLastAccessTime(LocalDateTime.now());
            log.error("标记会话错误: sessionId={}, error={}", sessionId, errorMessage);
        } else {
            log.warn("会话不存在，无法标记错误: sessionId={}", sessionId);
        }
    }

    /**
     * 更新会话最后访问时间
     * 用于保持会话活跃，防止被清理
     *
     * @param sessionId 会话ID
     * @author DuRuiChi
     */
    @Override
    public void updateLastAccessTime(String sessionId) {
        // 获取会话对象
        StreamSession session = sessionMap.get(sessionId);
        if (session != null) {
            // 更新最后访问时间（用于保持会话活跃，防止被清理）
            session.setLastAccessTime(LocalDateTime.now());
        }
    }

    /**
     * 清理过期会话
     * 定时任务，每5分钟执行一次，清理超过30分钟未访问的会话
     *
     * @author DuRuiChi
     */
    @Override
    @Scheduled(fixedRate = 300000)  // 每5分钟执行一次（300000毫秒）
    public void cleanExpiredSessions() {
        try {
            // 计算过期时间点（当前时间减去过期时长）
            LocalDateTime expireTime = LocalDateTime.now().minusMinutes(SESSION_EXPIRE_MINUTES);
            
            // 收集所有过期的会话ID（避免在遍历时直接删除导致并发问题）
            List<String> expiredSessionIds = new ArrayList<>();
            sessionMap.forEach((sessionId, session) -> {
                // 空值保护
                if (session == null || session.getLastAccessTime() == null) {
                    log.warn("发现异常会话数据，标记为过期: sessionId={}", sessionId);
                    expiredSessionIds.add(sessionId);
                    return;
                }
                
                // 如果会话的最后访问时间早于过期时间点，则标记为过期
                if (session.getLastAccessTime().isBefore(expireTime)) {
                    expiredSessionIds.add(sessionId);
                }
            });
            
            // 批量清理过期会话（先收集再删除，避免并发修改异常）
            int cleanedCount = 0;
            for (String sessionId : expiredSessionIds) {
                try {
                    // 从会话映射表中移除
                    StreamSession removedSession = sessionMap.remove(sessionId);
                    // 从事件ID计数器映射表中移除
                    AtomicLong removedCounter = eventIdCounters.remove(sessionId);
                    
                    if (removedSession != null) {
                        cleanedCount++;
                        log.info("清理过期会话: sessionId={}, appId={}, 事件数={}", 
                                sessionId, 
                                removedSession.getAppId(),
                                removedSession.getEventQueue() != null ? removedSession.getEventQueue().size() : 0);
                    }
                } catch (Exception e) {
                    log.error("清理会话失败: sessionId={}, error={}", sessionId, e.getMessage());
                }
            }
            
            // 记录清理结果
            if (cleanedCount > 0) {
                log.info("定时清理任务完成，清理了 {} 个过期会话，当前活跃会话数: {}", 
                        cleanedCount, sessionMap.size());
            }
        } catch (Exception e) {
            log.error("定时清理任务执行失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 生成事件ID
     * 为指定会话生成唯一的事件ID，格式为：sessionId-序号
     *
     * @param sessionId 会话ID
     * @return String 事件ID
     * @author DuRuiChi
     */
    @Override
    public String generateEventId(String sessionId) {
        // 参数校验：确保会话ID有效
        if (StrUtil.isBlank(sessionId)) {
            log.error("生成事件ID失败：会话ID为空");
            throw new IllegalArgumentException("会话ID不能为空");
        }
        
        // 获取或创建该会话的事件ID计数器（线程安全）
        AtomicLong counter = eventIdCounters.computeIfAbsent(sessionId, k -> new AtomicLong(0));
        
        // 生成唯一的事件ID：sessionId-序号（原子递增，线程安全）
        long eventSequence = counter.incrementAndGet();
        String eventId = sessionId + "-" + eventSequence;
        
        log.debug("生成事件ID: sessionId={}, eventId={}", sessionId, eventId);
        return eventId;
    }

    /**
     * 生成会话ID
     * 根据产物ID、用户ID和时间戳生成唯一的会话ID
     *
     * @param appId  产物ID
     * @param userId 用户ID
     * @return String 会话ID
     * @author DuRuiChi
     */
    private String generateSessionId(Long appId, Long userId) {
        // 参数校验（内部方法，双重保护）
        if (appId == null || appId <= 0 || userId == null || userId <= 0) {
            log.error("生成会话ID失败：参数无效 - appId={}, userId={}", appId, userId);
            throw new IllegalArgumentException("产物ID和用户ID不能为空或小于等于0");
        }
        
        // 生成唯一的会话ID：session-appId-userId-时间戳
        // 时间戳确保即使同一用户对同一产物的多次请求也能生成不同的会话ID（毫秒级精度）
        long timestamp = System.currentTimeMillis();
        String sessionId = String.format("session-%d-%d-%d", appId, userId, timestamp);
        
        log.debug("生成会话ID: sessionId={}, appId={}, userId={}", sessionId, appId, userId);
        return sessionId;
    }
}
