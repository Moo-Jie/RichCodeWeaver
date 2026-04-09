package com.rich.app.service;

import com.mybatisflex.core.service.IService;
import com.rich.model.entity.CustomerServiceMessage;
import com.rich.model.vo.CustomerServiceMessageVO;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 客服消息 Service
 *
 * @author DuRuiChi
 * @create 2026/4/9
 */
public interface CustomerServiceMessageService extends IService<CustomerServiceMessage> {

    CustomerServiceMessage addMessage(Long conversationId, String senderType, String content, Long userId, String visitorKey);

    List<CustomerServiceMessageVO> listConversationMessages(Long conversationId, int pageSize, LocalDateTime lastCreateTime);

    Boolean loadChatHistoryToMemory(Long conversationId, MessageWindowChatMemory chatMemory, int maxCount);
}
