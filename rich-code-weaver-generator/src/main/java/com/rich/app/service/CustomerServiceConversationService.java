package com.rich.app.service;

import com.mybatisflex.core.service.IService;
import com.rich.model.entity.CustomerServiceConversation;
import com.rich.model.vo.CustomerServiceConversationVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 客服会话 Service
 *
 * @author DuRuiChi
 * @create 2026/4/9
 */
public interface CustomerServiceConversationService extends IService<CustomerServiceConversation> {

    CustomerServiceConversation createConversation(Long userId, String visitorKey);

    CustomerServiceConversation verifyAndGetConversation(Long conversationId, Long userId, String visitorKey);

    List<CustomerServiceConversationVO> listUserConversations(Long userId, String visitorKey);

    void refreshConversation(Long conversationId, String preview, LocalDateTime messageTime, boolean updateTitle);
}
