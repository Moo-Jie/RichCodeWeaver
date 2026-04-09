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

    /**
     * 创建 AI 客服会话
     *
     * @param request 请求对象
     * @return 会话视图对象
     */
    CustomerServiceConversationVO createConversation(HttpServletRequest request);

    /**
     * 查询当前用户或游客的会话列表
     *
     * @param request 请求对象
     * @return 会话列表
     */
    List<CustomerServiceConversationVO> listConversations(HttpServletRequest request);

    /**
     * 分页查询指定会话的消息
     *
     * @param conversationId 会话 ID
     * @param pageSize 每页大小
     * @param lastCreateTime 上一页最后一条消息时间
     * @param request 请求对象
     * @return 消息列表
     */
    List<CustomerServiceMessageVO> listMessages(Long conversationId, int pageSize, LocalDateTime lastCreateTime,
                                                HttpServletRequest request);

    /**
     * 发起 AI 客服流式对话
     *
     * @param chatRequest 对话请求
     * @param request 请求对象
     * @return SSE 事件流
     */
    Flux<ServerSentEvent<String>> chatStream(CustomerServiceChatRequest chatRequest, HttpServletRequest request);
}
