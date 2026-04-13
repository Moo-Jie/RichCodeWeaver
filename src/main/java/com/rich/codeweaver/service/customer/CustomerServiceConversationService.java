package com.rich.codeweaver.service.customer;

import com.mybatisflex.core.service.IService;
import com.rich.codeweaver.model.entity.CustomerServiceConversation;
import com.rich.codeweaver.model.vo.CustomerServiceConversationVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 客服会话 Service
 *
 * @author DuRuiChi
 * @create 2026/4/9
 */
public interface CustomerServiceConversationService extends IService<CustomerServiceConversation> {

    /**
     * 创建会话记录
     *
     * @param userId 登录用户 ID
     * @param visitorKey 游客标识
     * @return 会话实体
     */
    CustomerServiceConversation createConversation(Long userId, String visitorKey);

    /**
     * 校验会话归属并返回会话实体
     *
     * @param conversationId 会话 ID
     * @param userId 登录用户 ID
     * @param visitorKey 游客标识
     * @return 会话实体
     */
    CustomerServiceConversation verifyAndGetConversation(Long conversationId, Long userId, String visitorKey);

    /**
     * 查询用户或游客的会话列表
     *
     * @param userId 登录用户 ID
     * @param visitorKey 游客标识
     * @return 会话列表
     */
    List<CustomerServiceConversationVO> listUserConversations(Long userId, String visitorKey);

    /**
     * 刷新会话摘要信息
     *
     * @param conversationId 会话 ID
     * @param preview 最新摘要
     * @param messageTime 最新消息时间
     * @param updateTitle 是否更新标题
     */
    void refreshConversation(Long conversationId, String preview, LocalDateTime messageTime, boolean updateTitle);
}
