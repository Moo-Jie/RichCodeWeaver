package com.rich.app.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.app.mapper.AiChatSummaryMapper;
import com.rich.app.service.AiChatSummaryService;
import com.rich.app.service.ChatHistoryService;
import com.rich.app.service.CustomerServiceMessageService;
import com.rich.client.innerService.InnerSystemPromptService;
import com.rich.common.constant.AiServiceConstant;
import com.rich.common.utils.SpringContextUtil;
import com.rich.model.entity.AiChatSummary;
import com.rich.model.entity.ChatHistory;
import com.rich.model.entity.CustomerServiceMessage;
import com.rich.model.enums.ChatHistoryTypeEnum;
import com.rich.model.enums.CustomerServiceMessageRoleEnum;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 对话增量摘要 服务层实现
 * 核心逻辑：
 * 1. 每轮对话完成后检查是否满足触发条件（累计新增满5轮）
 * 2. 满足时拉取历史摘要 + 最新5轮对话，调用 LLM 合并生成新摘要
 * 3. 新摘要落库，供后续 AI 服务构建时注入上下文
 *
 * @author DuRuiChi
 * @create 2026/5/17
 */
@Slf4j
@Service
public class AiChatSummaryServiceImpl extends ServiceImpl<AiChatSummaryMapper, AiChatSummary> implements AiChatSummaryService {

    @Resource
    @Lazy
    private ChatHistoryService chatHistoryService;

    @Resource
    @Lazy
    private CustomerServiceMessageService customerServiceMessageService;

    @DubboReference
    private InnerSystemPromptService innerSystemPromptService;

    /**
     * 异步触发增量摘要生成
     * 判断逻辑：当前总AI轮数 - 上次已摘要轮数 >= 5 时触发
     */
    @Async
    @Override
    public void tryTriggerSummary(Long memoryId, String summaryType) {
        try {
            if (memoryId == null || memoryId <= 0 || StrUtil.isBlank(summaryType)) {
                return;
            }

            // 统计当前会话的总 AI 响应轮数（每条 AI 消息 = 1轮）
            int currentRoundCount = countAiRounds(memoryId, summaryType);
            if (currentRoundCount < AiServiceConstant.SUMMARY_TRIGGER_ROUND_COUNT) {
                return;
            }

            // 查询已有摘要记录
            AiChatSummary existingSummary = getLatestSummary(memoryId, summaryType);
            int lastSummarizedCount = existingSummary != null ? existingSummary.getSummarizedRoundCount() : 0;

            // 判断是否达到触发阈值
            int pendingRounds = currentRoundCount - lastSummarizedCount;
            if (pendingRounds < AiServiceConstant.SUMMARY_TRIGGER_ROUND_COUNT) {
                return;
            }

            log.info("【增量摘要】触发生成: memoryId={}, summaryType={}, 当前轮数={}, 上次摘要轮数={}, 待摘要轮数={}",
                    memoryId, summaryType, currentRoundCount, lastSummarizedCount, pendingRounds);

            // 获取历史摘要内容
            String oldSummary = existingSummary != null ? existingSummary.getSummaryContent() : "";

            // 获取最新5轮对话内容（5条 USER + 5条 AI = 10条消息）
            String recentChatContent = buildRecentChatContent(memoryId, AiServiceConstant.SUMMARY_TRIGGER_ROUND_COUNT, summaryType);

            // 调用 LLM 生成新摘要
            String newSummary = generateSummaryByLlm(oldSummary, recentChatContent, memoryId);
            if (StrUtil.isBlank(newSummary)) {
                log.warn("【增量摘要】LLM 生成摘要为空: memoryId={}", memoryId);
                return;
            }

            // 持久化摘要
            saveSummary(existingSummary, memoryId, summaryType, newSummary, currentRoundCount);

            log.info("【增量摘要】生成完成: memoryId={}, summaryType={}, 新摘要长度={}",
                    memoryId, summaryType, newSummary.length());
        } catch (Exception e) {
            log.error("【增量摘要】生成异常: memoryId={}, summaryType={}, error={}",
                    memoryId, summaryType, e.getMessage(), e);
        }
    }

    @Override
    public String getLatestSummaryContent(Long memoryId, String summaryType) {
        if (memoryId == null || memoryId <= 0 || StrUtil.isBlank(summaryType)) {
            return null;
        }
        AiChatSummary summary = getLatestSummary(memoryId, summaryType);
        return summary != null ? summary.getSummaryContent() : null;
    }

    // ======================== 内部方法 ========================

    /**
     * 统计指定会话的 AI 响应轮数
     * Agent/Workflow 查 chat_history 表，CustomerService 查 customer_service_message 表
     */
    private int countAiRounds(Long memoryId, String summaryType) {
        if (AiServiceConstant.SUMMARY_TYPE_CUSTOMER_SERVICE.equals(summaryType)) {
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(CustomerServiceMessage::getConversationId, memoryId)
                    .eq(CustomerServiceMessage::getSenderType, CustomerServiceMessageRoleEnum.AI.getValue());
            return (int) customerServiceMessageService.count(queryWrapper);
        } else {
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, memoryId)
                    .eq(ChatHistory::getMessageType, ChatHistoryTypeEnum.AI.getValue());
            return (int) chatHistoryService.count(queryWrapper);
        }
    }

    /**
     * 获取最新摘要记录
     */
    private AiChatSummary getLatestSummary(Long memoryId, String summaryType) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(AiChatSummary::getMemoryId, memoryId)
                .eq(AiChatSummary::getSummaryType, summaryType)
                .orderBy(AiChatSummary::getUpdateTime, false)
                .limit(1);
        return this.getOne(queryWrapper);
    }

    /**
     * 构建最近 N 轮对话内容文本（用于提交给 LLM 做摘要）
     * Agent/Workflow 查 chat_history 表，CustomerService 查 customer_service_message 表
     */
    private String buildRecentChatContent(Long memoryId, int roundCount, String summaryType) {
        int messageLimit = roundCount * 2;

        if (AiServiceConstant.SUMMARY_TYPE_CUSTOMER_SERVICE.equals(summaryType)) {
            return buildCustomerServiceRecentChat(memoryId, messageLimit);
        } else {
            return buildChatHistoryRecentChat(memoryId, messageLimit);
        }
    }

    /**
     * 从 chat_history 表构建最近对话文本（Agent/Workflow）
     */
    private String buildChatHistoryRecentChat(Long memoryId, int messageLimit) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(ChatHistory::getAppId, memoryId)
                .orderBy(ChatHistory::getCreateTime, false)
                .limit(messageLimit);

        List<ChatHistory> recentMessages = chatHistoryService.list(queryWrapper);
        if (recentMessages == null || recentMessages.isEmpty()) {
            return "";
        }
        recentMessages = recentMessages.reversed();

        StringBuilder sb = new StringBuilder();
        for (ChatHistory msg : recentMessages) {
            if (StrUtil.isBlank(msg.getMessage())) {
                continue;
            }
            String role = ChatHistoryTypeEnum.USER.getValue().equals(msg.getMessageType()) ? "用户" : "AI";
            sb.append(role).append("：").append(msg.getMessage()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 从 customer_service_message 表构建最近对话文本（CustomerService）
     */
    private String buildCustomerServiceRecentChat(Long memoryId, int messageLimit) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(CustomerServiceMessage::getConversationId, memoryId)
                .orderBy(CustomerServiceMessage::getCreateTime, false)
                .limit(messageLimit);

        List<CustomerServiceMessage> recentMessages = customerServiceMessageService.list(queryWrapper);
        if (recentMessages == null || recentMessages.isEmpty()) {
            return "";
        }
        recentMessages = recentMessages.reversed();

        StringBuilder sb = new StringBuilder();
        for (CustomerServiceMessage msg : recentMessages) {
            if (StrUtil.isBlank(msg.getContent())) {
                continue;
            }
            String role = CustomerServiceMessageRoleEnum.USER.getValue().equals(msg.getSenderType()) ? "用户" : "AI";
            sb.append(role).append("：").append(msg.getContent()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 调用 LLM 生成摘要
     */
    private String generateSummaryByLlm(String oldSummary, String recentChatContent, Object memoryId) {
        // 获取摘要生成系统提示词
        String systemPromptContent = innerSystemPromptService.getPromptContentByKey(
                AiServiceConstant.SUMMARY_SYSTEM_PROMPT_KEY);
        if (StrUtil.isBlank(systemPromptContent)) {
            log.error("【增量摘要】未找到系统提示词: promptKey={}", AiServiceConstant.SUMMARY_SYSTEM_PROMPT_KEY);
            return null;
        }

        // 构建用户输入：历史摘要 + 新增对话
        StringBuilder userInput = new StringBuilder();
        userInput.append("整合内容：\n");
        if (StrUtil.isNotBlank(oldSummary)) {
            userInput.append("【历史摘要】\n").append(oldSummary).append("\n\n");
        }
        userInput.append("【新增对话】\n").append(recentChatContent);

        // 获取摘要专用 ChatModel
        ChatModel summaryChatModel = SpringContextUtil.getBean(
                AiServiceConstant.SUMMARY_CHAT_MODEL_BEAN, ChatModel.class);

        try {
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(List.of(
                            SystemMessage.from(systemPromptContent),
                            UserMessage.from(userInput.toString())
                    ))
                    .build();
            ChatResponse chatResponse = summaryChatModel.chat(chatRequest);
            AiMessage aiMessage = chatResponse.aiMessage();
            return aiMessage != null ? aiMessage.text() : null;
        } catch (Exception e) {
            log.error("【增量摘要】LLM 调用失败: memoryId={}, error={}", memoryId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 持久化摘要（新增或更新）
     */
    private void saveSummary(AiChatSummary existingSummary, Long memoryId, String summaryType,
                             String newSummaryContent, int currentRoundCount) {
        if (existingSummary != null) {
            // 更新已有记录
            existingSummary.setSummaryContent(newSummaryContent);
            existingSummary.setSummarizedRoundCount(currentRoundCount);
            this.updateById(existingSummary);
        } else {
            // 首次创建
            AiChatSummary newSummary = AiChatSummary.builder()
                    .memoryId(memoryId)
                    .summaryType(summaryType)
                    .summaryContent(newSummaryContent)
                    .summarizedRoundCount(currentRoundCount)
                    .build();
            this.save(newSummary);
        }
    }
}
