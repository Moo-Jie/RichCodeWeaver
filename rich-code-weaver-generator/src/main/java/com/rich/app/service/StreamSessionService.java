package com.rich.app.service;

import com.rich.app.model.StreamEvent;
import com.rich.app.model.StreamSession;

import java.util.List;

/**
 * SSE 流式会话管理服务接口
 *
 * @author DuRuiChi
 * @create 2025/12/27
 **/
public interface StreamSessionService {

    /**
     * 创建新的流式会话
     *
     * @param appId  产物ID
     * @param userId 用户ID
     * @return 会话ID
     */
    String createSession(Long appId, Long userId);

    /**
     * 创建新的流式会话（使用自定义会话ID）
     *
     * @param sessionId 自定义会话ID
     * @param appId     产物ID
     * @param userId    用户ID
     * @return 会话ID
     */
    String createSession(String sessionId, Long appId, Long userId);

    /**
     * 获取会话
     *
     * @param sessionId 会话ID
     * @return 会话对象
     */
    StreamSession getSession(String sessionId);

    /**
     * 添加事件到会话
     *
     * @param sessionId 会话ID
     * @param event     事件对象
     */
    void addEvent(String sessionId, StreamEvent event);

    /**
     * 获取指定事件ID之后的所有事件
     *
     * @param sessionId   会话ID
     * @param lastEventId 最后接收到的事件ID
     * @return 事件列表
     */
    List<StreamEvent> getEventsAfter(String sessionId, String lastEventId);

    /**
     * 标记会话完成
     *
     * @param sessionId 会话ID
     */
    void markCompleted(String sessionId);

    /**
     * 标记会话错误
     *
     * @param sessionId    会话ID
     * @param errorMessage 错误信息
     */
    void markError(String sessionId, String errorMessage);

    /**
     * 更新会话最后访问时间
     *
     * @param sessionId 会话ID
     */
    void updateLastAccessTime(String sessionId);

    /**
     * 清理过期会话
     */
    void cleanExpiredSessions();

    /**
     * 生成下一个事件ID
     *
     * @param sessionId 会话ID
     * @return 事件ID
     */
    String generateEventId(String sessionId);
}
