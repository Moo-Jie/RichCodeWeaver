package com.rich.ai.memory.chat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 带截断功能的 ChatMemoryStore 装饰器
 * 在写入 Redis 前对超长消息进行截断，防止 Redis bigkey 问题
 *
 * - 仅截断 AiMessage 和 ToolExecutionResultMessage（最容易膨胀的两类）
 * - UserMessage / SystemMessage 通常较小，不做截断
 * - 截断仅影响 Redis 中的短期记忆，完整消息仍保存在数据库中
 *
 * @author DuRuiChi
 * @create 2026/5/16
 */
@Slf4j
public class TruncatingChatMemoryStore implements ChatMemoryStore {

    /**
     * 被装饰的底层存储（通常为 RedisChatMemoryStore）
     */
    private final ChatMemoryStore delegate;

    /**
     * 单条消息最大字符数
     */
    private final int maxMessageChars;

    /**
     * 截断后追加的后缀标记
     */
    private final String truncationSuffix;

    public TruncatingChatMemoryStore(ChatMemoryStore delegate, int maxMessageChars, String truncationSuffix) {
        this.delegate = delegate;
        this.maxMessageChars = maxMessageChars;
        this.truncationSuffix = truncationSuffix;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        return delegate.getMessages(memoryId);
    }

    /**
     * 写入前对每条消息做截断检查，防止 bigkey
     */
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        List<ChatMessage> safeMessages = new ArrayList<>(messages.size());
        for (ChatMessage message : messages) {
            safeMessages.add(truncateIfNeeded(message, memoryId));
        }
        delegate.updateMessages(memoryId, safeMessages);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        delegate.deleteMessages(memoryId);
    }

    /**
     * 对超长消息进行截断
     * 仅处理 AiMessage 和 ToolExecutionResultMessage，其余类型直接放行
     */
    private ChatMessage truncateIfNeeded(ChatMessage message, Object memoryId) {
        if (message instanceof AiMessage aiMsg) {
            return truncateAiMessage(aiMsg, memoryId);
        }
        if (message instanceof ToolExecutionResultMessage toolMsg) {
            return truncateToolResultMessage(toolMsg, memoryId);
        }
        return message;
    }

    /**
     * 截断 AI 消息（保留 toolExecutionRequests 不变，仅截断 text 部分）
     */
    private ChatMessage truncateAiMessage(AiMessage aiMsg, Object memoryId) {
        String text = aiMsg.text();
        if (text == null || text.length() <= maxMessageChars) {
            return aiMsg;
        }
        log.info("截断 AiMessage: memoryId={}, 原长度={}, 截断至={}",
                memoryId, text.length(), maxMessageChars);
        String truncatedText = text.substring(0, maxMessageChars) + truncationSuffix;
        return new AiMessage(truncatedText, aiMsg.toolExecutionRequests());
    }

    /**
     * 截断工具执行结果消息（保留 id 和 toolName，仅截断 text 部分）
     */
    private ChatMessage truncateToolResultMessage(ToolExecutionResultMessage toolMsg, Object memoryId) {
        String text = toolMsg.text();
        if (text == null || text.length() <= maxMessageChars) {
            return toolMsg;
        }
        log.info("截断 ToolExecutionResultMessage: memoryId={}, toolName={}, 原长度={}, 截断至={}",
                memoryId, toolMsg.toolName(), text.length(), maxMessageChars);
        String truncatedText = text.substring(0, maxMessageChars) + truncationSuffix;
        return new ToolExecutionResultMessage(toolMsg.id(), toolMsg.toolName(), truncatedText);
    }
}
