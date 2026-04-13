package com.rich.codeweaver.service.user;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.rich.codeweaver.model.entity.ChatMessage;
import com.rich.codeweaver.model.vo.ChatMessageVO;

/**
 * 聊天消息 服务层
 * 提供消息发送、查询、已读标记等业务逻辑
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
public interface ChatMessageService extends IService<ChatMessage> {

    /**
     * 发送消息
     *
     * @param senderId    发送方用户id
     * @param receiverId  接收方用户id
     * @param content     消息内容
     * @param messageType 消息类型
     * @return 消息VO
     */
    ChatMessageVO sendMessage(Long senderId, Long receiverId, String content, String messageType);

    /**
     * 分页查询会话消息（按时间升序）
     *
     * @param conversationId 会话id
     * @param currentUserId  当前登录用户id
     * @param pageNum        页码
     * @param pageSize       每页数量
     * @return 消息分页
     */
    Page<ChatMessageVO> listMessages(Long conversationId, Long currentUserId, long pageNum, long pageSize);

    /**
     * 标记会话中接收到的消息为已读
     *
     * @param conversationId 会话id
     * @param userId         当前用户id（标记自己收到的消息为已读）
     */
    void markAsRead(Long conversationId, Long userId);

    /**
     * 获取用户总未读消息数
     *
     * @param userId 用户id
     * @return 未读消息总数
     */
    int getUnreadCount(Long userId);
}
