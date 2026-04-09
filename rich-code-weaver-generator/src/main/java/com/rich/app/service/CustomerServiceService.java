package com.rich.app.service;

import com.rich.model.dto.customerservice.CustomerServiceChatRequest;
import com.rich.model.vo.CustomerServiceConversationVO;
import com.rich.model.vo.CustomerServiceMessageVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 客服聚合 Service
 *
 * @author DuRuiChi
 * @create 2026/4/9
 */
public interface CustomerServiceService {

    CustomerServiceConversationVO createConversation(HttpServletRequest request);

    List<CustomerServiceConversationVO> listConversations(HttpServletRequest request);

    List<CustomerServiceMessageVO> listMessages(Long conversationId, int pageSize, LocalDateTime lastCreateTime,
                                                HttpServletRequest request);

    Flux<ServerSentEvent<String>> chatStream(CustomerServiceChatRequest chatRequest, HttpServletRequest request);
}
