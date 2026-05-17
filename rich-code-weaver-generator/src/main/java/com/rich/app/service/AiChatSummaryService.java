package com.rich.app.service;

import com.mybatisflex.core.service.IService;
import com.rich.model.entity.AiChatSummary;

/**
 * AI 对话增量摘要 服务层
 * 负责摘要的触发判断、LLM 生成、持久化与读取
 *
 * @author DuRuiChi
 * @create 2026/5/17
 */
public interface AiChatSummaryService extends IService<AiChatSummary> {

    /**
     * 对话完成后尝试触发增量摘要生成（异步）
     * 判断当前累计轮数是否达到阈值，满足则调用 LLM 生成新摘要并落库
     *
     * @param memoryId    记忆标识（appId 或 conversationId）
     * @param summaryType 摘要类型（AGENT / WORKFLOW / CUSTOMER_SERVICE）
     */
    void tryTriggerSummary(Long memoryId, String summaryType);

    /**
     * 获取指定会话的最新摘要内容
     * 用于在构建 AI 服务时注入上下文
     *
     * @param memoryId    记忆标识
     * @param summaryType 摘要类型
     * @return 摘要内容（无摘要时返回 null）
     */
    String getLatestSummaryContent(Long memoryId, String summaryType);
}
