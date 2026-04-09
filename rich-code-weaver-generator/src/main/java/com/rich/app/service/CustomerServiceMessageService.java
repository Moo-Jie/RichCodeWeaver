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

    /**
     * 新增一条会话消息
     *
     * @param conversationId 会话 ID
     * @param senderType 发送方类型
     * @param content 消息内容
     * @param userId 登录用户 ID
     * @param visitorKey 游客标识
     * @return 消息实体
     */
    CustomerServiceMessage addMessage(Long conversationId, String senderType, String content, Long userId, String visitorKey);

    /**
     * 查询指定会话的消息列表
     *
     * @param conversationId 会话 ID
     * @param pageSize 每页大小
     * @param lastCreateTime 上一页最后一条消息时间
     * @return 消息列表
     */
    List<CustomerServiceMessageVO> listConversationMessages(Long conversationId, int pageSize, LocalDateTime lastCreateTime);

    /**
     * 将历史消息加载到 AI 记忆窗口
     *
     * @param conversationId 会话 ID
     * @param chatMemory 对话记忆
     * @param maxCount 最大加载数量
     * @return 是否加载成功
     */
    Boolean loadChatHistoryToMemory(Long conversationId, MessageWindowChatMemory chatMemory, int maxCount);
}
