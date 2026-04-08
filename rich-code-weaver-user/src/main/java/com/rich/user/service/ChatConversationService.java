package com.rich.user.service;

import com.mybatisflex.core.service.IService;
import com.rich.model.entity.ChatConversation;
import com.rich.model.vo.ChatConversationVO;

import java.util.List;

/**
 * 聊天会话 服务层
 * 提供会话的创建、查询等业务逻辑
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
public interface ChatConversationService extends IService<ChatConversation> {

    /**
     * 获取或创建两个用户之间的会话
     *
     * @param userId1 用户1的id
     * @param userId2 用户2的id
     * @return 会话实体
     */
    ChatConversation getOrCreateConversation(Long userId1, Long userId2);

    /**
     * 获取用户的所有会话列表（按最后消息时间倒序）
     *
     * @param userId 当前用户id
     * @return 会话列表VO
     */
    List<ChatConversationVO> listConversations(Long userId);

    /**
     * 更新会话的最后消息信息
     *
     * @param conversationId 会话id
     * @param messageId      消息id
     */
    void updateLastMessage(Long conversationId, Long messageId);
}
