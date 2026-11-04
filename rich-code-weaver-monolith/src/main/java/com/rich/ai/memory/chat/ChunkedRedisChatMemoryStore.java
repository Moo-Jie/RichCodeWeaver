package com.rich.ai.memory.chat;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static dev.langchain4j.data.message.ChatMessageDeserializer.messageFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messageToJson;

/**
 * 分片式Redis聊天记忆存储核心类
 * 核心功能：
 * 1. 版本管理：原子性更新聊天记录，避免读写冲突
 * 2. 消息分片：拆分超长消息，解决Redis值大小限制
 * 3. 工具折叠：压缩旧工具调用信息，节省Token
 * 4. 消息截断：强制压缩超长消息，保证存储稳定
 *
 * @author DuRuiChi
 */
@Slf4j
public class ChunkedRedisChatMemoryStore implements ChatMemoryStore {

    //  Redis 键前缀常量定义 
    /** 聊天记忆Redis键统一前缀 */
    private static final String KEY_PREFIX = "ai:chat_memory:";
    /** 活跃版本号后缀：标记当前使用的记忆版本 */
    private static final String ACTIVE_VERSION_KEY_SUFFIX = ":active_version";
    /** 版本计数器后缀：自增生成新版本号 */
    private static final String VERSION_COUNTER_KEY_SUFFIX = ":version_counter";
    /** 版本分隔符：拼接版本号 */
    private static final String VERSION_KEY_SEGMENT = ":v:";
    /** 消息索引后缀：存储当前版本下所有消息的序号列表 */
    private static final String INDEX_KEY_SUFFIX = ":index";
    /** 消息分片数量后缀：存储单条消息的分片总数 */
    private static final String CHUNK_COUNT_KEY_SUFFIX = ":chunk_count";
    /** 消息序号分隔符：拼接单条消息的索引 */
    private static final String MESSAGE_KEY_SEGMENT = ":msg:";
    /** 分片序号分隔符：拼接消息的分片索引 */
    private static final String CHUNK_KEY_SEGMENT = ":chunk:";

    //  核心配置依赖 
    /** Redis操作模板 */
    private final StringRedisTemplate stringRedisTemplate;
    /** 记忆过期时间（秒） */
    private final long ttlSeconds;
    /** 单个消息分片的最大字符长度 */
    private final int chunkSize;
    /** 单条消息允许的最大分片数 */
    private final int maxChunksPerMessage;
    /** 单条消息的最大字符数（截断阈值） */
    private final int maxMessageChars;
    /** 消息截断后的后缀（如...） */
    private final String truncationSuffix;
    /** 工具消息折叠：保留最近N组工具调用不折叠 */
    private final int toolFoldKeepRecent;
    /** 工具执行结果折叠后的替代文本 */
    private final String toolFoldedResult;
    /** 工具调用参数折叠后的替代文本 */
    private final String toolFoldedArguments;

    /**
     * 构造函数：初始化分片存储所有配置
     * @param stringRedisTemplate Redis客户端
     * @param ttlSeconds 过期时间
     * @param chunkSize 单分片大小
     * @param maxChunksPerMessage 单消息最大分片数
     * @param maxMessageChars 单消息最大字符数
     * @param truncationSuffix 截断后缀
     * @param toolFoldKeepRecent 保留最近N组工具调用
     * @param toolFoldedResult 工具结果折叠文本
     * @param toolFoldedArguments 工具参数折叠文本
     */
    public ChunkedRedisChatMemoryStore(StringRedisTemplate stringRedisTemplate,
                                       long ttlSeconds,
                                       int chunkSize,
                                       int maxChunksPerMessage,
                                       int maxMessageChars,
                                       String truncationSuffix,
                                       int toolFoldKeepRecent,
                                       String toolFoldedResult,
                                       String toolFoldedArguments) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.ttlSeconds = ttlSeconds;
        this.chunkSize = chunkSize;
        this.maxChunksPerMessage = maxChunksPerMessage;
        this.maxMessageChars = maxMessageChars;
        this.truncationSuffix = truncationSuffix;
        this.toolFoldKeepRecent = toolFoldKeepRecent;
        this.toolFoldedResult = toolFoldedResult;
        this.toolFoldedArguments = toolFoldedArguments;
    }

    /**
     * 获取指定会话的所有聊天消息
     * 逻辑：读取活跃版本 → 读取消息索引 → 拼接所有消息分片 → 反序列化为消息对象
     */
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        // 构建会话基础Redis键
        String baseKey = buildBaseKey(memoryId);
        // 读取当前活跃版本号
        String activeVersion = stringRedisTemplate.opsForValue().get(buildActiveVersionKey(baseKey));
        
        // 无版本=无记忆，返回空列表
        if (StrUtil.isBlank(activeVersion)) {
            return Collections.emptyList();
        }

        // 读取当前版本的消息索引列表（存储所有消息的序号）
        String indexKey = buildIndexKey(baseKey, activeVersion);
        List<String> messageIndexes = stringRedisTemplate.opsForList().range(indexKey, 0, -1);
        if (CollUtil.isEmpty(messageIndexes)) {
            return Collections.emptyList();
        }

        // 遍历索引，读取并拼接每条消息的所有分片
        List<ChatMessage> messages = new ArrayList<>(messageIndexes.size());
        for (String messageIndex : messageIndexes) {
            ChatMessage message = readMessage(baseKey, activeVersion, messageIndex);
            if (message != null) {
                messages.add(message);
            }
        }
        return messages;
    }

    /**
     * 更新会话的聊天消息（核心方法：包含版本管理+工具折叠+分片存储）
     * 逻辑：工具消息折叠 → 生成新版本号 → 分片存储新消息 → 切换活跃版本 → 删除旧版本
     */
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String baseKey = buildBaseKey(memoryId);
        
        // 空消息=直接删除记忆
        if (CollUtil.isEmpty(messages)) {
            deleteMessages(memoryId);
            return;
        }

        //  工具消息折叠（节省Token）
        List<ChatMessage> foldedMessages = foldToolMessages(messages, memoryId);

        //  版本管理（原子性更新）
        String activeVersionKey = buildActiveVersionKey(baseKey);
        String versionCounterKey = buildVersionCounterKey(baseKey);
        // 获取旧版本号（用于后续删除）
        String oldVersion = stringRedisTemplate.opsForValue().get(activeVersionKey);
        // Redis自增生成新版本号（保证唯一、有序）
        Long versionNumber = stringRedisTemplate.opsForValue().increment(versionCounterKey);
        if (versionNumber == null) {
            throw new IllegalStateException("生成 ChatMemory 版本号失败: memoryId=" + memoryId);
        }
        String newVersion = String.valueOf(versionNumber);

        //  分片存储：将折叠后的消息拆分分片存入Redis 
        String indexKey = buildIndexKey(baseKey, newVersion);
        for (int i = 0; i < foldedMessages.size(); i++) {
            // 预处理消息（超长截断）
            ChatMessage preparedMessage = prepareMessageForStorage(foldedMessages.get(i), memoryId);
            // 序列化为JSON
            String messageJson = messageToJson(preparedMessage);
            // 拆分为多个分片
            List<String> chunks = splitIntoChunks(messageJson);

            // 记录消息索引
            stringRedisTemplate.opsForList().rightPush(indexKey, String.valueOf(i));
            // 记录单条消息的分片总数
            String chunkCountKey = buildChunkCountKey(baseKey, newVersion, i);
            setValue(chunkCountKey, String.valueOf(chunks.size()));

            // 逐一分片存储
            for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
                String chunkKey = buildChunkKey(baseKey, newVersion, i, chunkIndex);
                setValue(chunkKey, chunks.get(chunkIndex));
            }
        }

        // 设置过期时间
        expireKey(indexKey);
        //  版本切换：将活跃版本指向新版本（原子性生效） 
        setValue(activeVersionKey, newVersion);
        expireKey(versionCounterKey);

        //  清理旧版本：删除历史版本的所有数据 
        if (StrUtil.isNotBlank(oldVersion) && !StrUtil.equals(oldVersion, newVersion)) {
            deleteVersion(baseKey, oldVersion);
        }
    }

    /**
     * 删除指定会话的所有聊天记忆
     * 逻辑：删除活跃版本数据 → 删除版本计数器 → 清理所有Redis键
     */
    @Override
    public void deleteMessages(Object memoryId) {
        String baseKey = buildBaseKey(memoryId);
        String activeVersionKey = buildActiveVersionKey(baseKey);
        String versionCounterKey = buildVersionCounterKey(baseKey);
        
        // 删除活跃版本的所有数据
        String activeVersion = stringRedisTemplate.opsForValue().get(activeVersionKey);
        if (StrUtil.isNotBlank(activeVersion)) {
            deleteVersion(baseKey, activeVersion);
        }
        
        // 删除版本管理相关键
        stringRedisTemplate.delete(List.of(activeVersionKey, versionCounterKey));
    }

    /**
     * 读取单条消息：根据版本、消息索引，拼接所有分片并反序列化
     */
    private ChatMessage readMessage(String baseKey, String version, String messageIndex) {
        // 获取消息的分片数量
        String chunkCountValue = stringRedisTemplate.opsForValue().get(buildChunkCountKey(baseKey, version, messageIndex));
        int chunkCount = parseInteger(chunkCountValue);
        if (chunkCount <= 0) {
            return null;
        }

        // 拼接所有分片
        StringBuilder messageJsonBuilder = new StringBuilder(chunkCount * chunkSize);
        for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
            String chunk = stringRedisTemplate.opsForValue().get(buildChunkKey(baseKey, version, messageIndex, chunkIndex));
            if (chunk == null) {
                log.warn("读取 ChatMemory 分片失败: memoryBaseKey={}, version={}, messageIndex={}, chunkIndex={}",
                        baseKey, version, messageIndex, chunkIndex);
                return null;
            }
            messageJsonBuilder.append(chunk);
        }

        // 反序列化为消息对象
        try {
            return messageFromJson(messageJsonBuilder.toString());
        } catch (Exception e) {
            log.error("反序列化 ChatMemory 消息失败: memoryBaseKey={}, version={}, messageIndex={}",
                    baseKey, version, messageIndex, e);
            return null;
        }
    }

    /**
     * 折叠旧的工具调用消息，节省Token消耗
     */
    private List<ChatMessage> foldToolMessages(List<ChatMessage> messages, Object memoryId) {
        // 关闭折叠功能，直接返回
        if (toolFoldKeepRecent <= 0) {
            return messages;
        }

        // 1.从后往前遍历，找到需要折叠的边界
        int toolGroupCount = 0;   // 工具调用组计数器
        int foldBoundary = -1;    // 折叠边界：<=边界的消息需要折叠
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            // 识别：包含工具请求的AI消息 = 一组工具调用的开始
            if (msg instanceof AiMessage aiMsg
                    && aiMsg.toolExecutionRequests() != null
                    && !aiMsg.toolExecutionRequests().isEmpty()) {
                toolGroupCount++;
                // 超过保留数量，标记折叠边界
                if (toolGroupCount > toolFoldKeepRecent) {
                    foldBoundary = i;
                    break;
                }
            }
        }

        // 无需要折叠的消息，直接返回
        if (foldBoundary < 0) {
            return messages;
        }

        List<ChatMessage> result = new ArrayList<>(messages.size());
        int foldedToolResultCount = 0;  // 统计折叠的工具结果数
        int foldedToolRequestCount = 0; // 统计折叠的工具请求数

        // 2.遍历所有消息，执行折叠
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            // 遍历折叠区域（i <= foldBoundary）旧工具消息，执行压缩
            if (i <= foldBoundary) {
                if (msg instanceof ToolExecutionResultMessage toolMsg) {
                    // 折叠工具执行结果：替换为固定文本（toolFoldedResult）
                    result.add(new ToolExecutionResultMessage(
                            toolMsg.id(), toolMsg.toolName(), toolFoldedResult));
                    foldedToolResultCount++;
                } else if (msg instanceof AiMessage aiMsg
                        && aiMsg.toolExecutionRequests() != null
                        && !aiMsg.toolExecutionRequests().isEmpty()) {
                    // 折叠工具调用参数：保留ID/名称，参数替换为固定文本
                    List<ToolExecutionRequest> foldedRequests = aiMsg.toolExecutionRequests().stream()
                            .map(req -> ToolExecutionRequest.builder()
                                    .id(req.id())
                                    .name(req.name())
                                    .arguments(toolFoldedArguments)
                                    .build())
                            .toList();
                    result.add(new AiMessage(aiMsg.text(), foldedRequests));
                    foldedToolRequestCount++;
                } else {
                    // 非工具消息：用户消息/纯文本AI消息，不折叠
                    result.add(msg);
                }
            } else {
                //  非折叠区域：最近的工具消息，保留原样 
                result.add(msg);
            }
        }

        // 打印折叠日志
        if (foldedToolResultCount > 0 || foldedToolRequestCount > 0) {
            log.info("【工具消息折叠】memoryId={}, 折叠工具结果={}条, 折叠工具请求={}条, 保留最近{}组不折叠",
                    memoryId, foldedToolResultCount, foldedToolRequestCount, toolFoldKeepRecent);
        }

        return result;
    }

    /**
     * 删除一个版本的所有聊天记忆数据（彻底清理Redis）
     * 清理范围：消息索引、消息分片数、所有消息分片
     */
    private void deleteVersion(String baseKey, String version) {
        // 1. 获取该版本的消息索引
        String indexKey = buildIndexKey(baseKey, version);
        List<String> messageIndexes = stringRedisTemplate.opsForList().range(indexKey, 0, -1);
        
        // 2. 构建所有需要删除的Redis键
        List<String> keysToDelete = new ArrayList<>();
        keysToDelete.add(indexKey); // 删除索引
        
        if (CollUtil.isNotEmpty(messageIndexes)) {
            for (String messageIndex : messageIndexes) {
                // 删除消息分片数量键
                String chunkCountKey = buildChunkCountKey(baseKey, version, messageIndex);
                keysToDelete.add(chunkCountKey);
                
                // 删除该消息的所有分片
                int chunkCount = parseInteger(stringRedisTemplate.opsForValue().get(chunkCountKey));
                for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
                    keysToDelete.add(buildChunkKey(baseKey, version, messageIndex, chunkIndex));
                }
            }
        }

        // 3. 批量删除Redis键
        stringRedisTemplate.delete(keysToDelete);
        log.debug("【版本管理】删除旧版本记忆: baseKey={}, version={}", baseKey, version);
    }

    /**
     * 消息存储前预处理：如果超过最大分片数，自动截断/压缩
     */
    private ChatMessage prepareMessageForStorage(ChatMessage message, Object memoryId) {
        ChatMessage currentMessage = message;
        String currentJson = messageToJson(currentMessage);
        int currentChunkCount = calculateChunkCount(currentJson.length());
        
        // 符合分片要求，直接返回
        if (currentChunkCount <= maxChunksPerMessage) {
            return currentMessage;
        }

        // 尝试3次软截断
        for (int attempt = 0; attempt < 3 && currentChunkCount > maxChunksPerMessage; attempt++) {
            ChatMessage truncatedMessage = truncateMessage(currentMessage, memoryId, currentJson.length());
            if (truncatedMessage == currentMessage) {
                break;
            }
            currentMessage = truncatedMessage;
            currentJson = messageToJson(currentMessage);
            currentChunkCount = calculateChunkCount(currentJson.length());
        }

        // 软截断失败，强制极限压缩
        if (currentChunkCount > maxChunksPerMessage) {
            currentMessage = forceCompactMessage(currentMessage, memoryId, currentJson.length());
            currentJson = messageToJson(currentMessage);
            currentChunkCount = calculateChunkCount(currentJson.length());
        }

        // 仍然超限，打印警告
        if (currentChunkCount > maxChunksPerMessage) {
            log.warn("ChatMemory 消息在强制压缩后仍然超过分片上限: memoryId={}, jsonLength={}, chunkCount={}",
                    memoryId, currentJson.length(), currentChunkCount);
        }
        return currentMessage;
    }

    /**
     * 软截断：裁剪消息文本，保留核心内容
     */
    private ChatMessage truncateMessage(ChatMessage message, Object memoryId, int serializedLength) {
        if (message instanceof AiMessage aiMessage) {
            String truncatedText = truncateText(aiMessage.text(), serializedLength);
            log.info("截断 AiMessage: memoryId={}, 原序列化长度={}, 截断后正文长度={}",
                    memoryId, serializedLength, truncatedText.length());
            return new AiMessage(truncatedText, aiMessage.toolExecutionRequests());
        }
        if (message instanceof ToolExecutionResultMessage toolMessage) {
            String truncatedText = truncateText(toolMessage.text(), serializedLength);
            log.info("截断 ToolExecutionResultMessage: memoryId={}, toolName={}, 原序列化长度={}, 截断后正文长度={}",
                    memoryId, toolMessage.toolName(), serializedLength, truncatedText.length());
            return new ToolExecutionResultMessage(toolMessage.id(), toolMessage.toolName(), truncatedText);
        }
        if (message instanceof UserMessage userMessage) {
            String truncatedText = truncateText(userMessage.singleText(), serializedLength);
            log.info("截断 UserMessage: memoryId={}, 原序列化长度={}, 截断后正文长度={}",
                    memoryId, serializedLength, truncatedText.length());
            return UserMessage.from(truncatedText);
        }
        return message;
    }

    /**
     * 强制压缩：极限缩短消息文本（兜底策略）
     */
    private ChatMessage forceCompactMessage(ChatMessage message, Object memoryId, int serializedLength) {
        if (message instanceof AiMessage aiMessage) {
            String compactText = buildCompactText(aiMessage.text());
            log.warn("强制压缩 AiMessage: memoryId={}, 原序列化长度={}, 压缩后正文长度={}",
                    memoryId, serializedLength, compactText.length());
            return new AiMessage(compactText, aiMessage.toolExecutionRequests());
        }
        if (message instanceof ToolExecutionResultMessage toolMessage) {
            String compactText = buildCompactText(toolMessage.text());
            log.warn("强制压缩 ToolExecutionResultMessage: memoryId={}, toolName={}, 原序列化长度={}, 压缩后正文长度={}",
                    memoryId, toolMessage.toolName(), serializedLength, compactText.length());
            return new ToolExecutionResultMessage(toolMessage.id(), toolMessage.toolName(), compactText);
        }
        if (message instanceof UserMessage userMessage) {
            String compactText = buildCompactText(userMessage.singleText());
            log.warn("强制压缩 UserMessage: memoryId={}, 原序列化长度={}, 压缩后正文长度={}",
                    memoryId, serializedLength, compactText.length());
            return UserMessage.from(compactText);
        }
        return message;
    }

    /**
     * 计算截断后的文本
     */
    private String truncateText(String text, int serializedLength) {
        if (StrUtil.isBlank(text)) {
            return text;
        }
        int maxSerializedLength = chunkSize * maxChunksPerMessage;
        int overheadLength = Math.max(0, serializedLength - text.length());
        int dynamicTextLimit = maxSerializedLength - overheadLength - truncationSuffix.length();
        int finalTextLimit = Math.min(maxMessageChars, dynamicTextLimit);
        if (finalTextLimit <= 0) {
            finalTextLimit = Math.min(maxMessageChars, Math.max(1, chunkSize));
        }
        if (text.length() <= finalTextLimit) {
            return text;
        }
        return text.substring(0, finalTextLimit) + truncationSuffix;
    }

    /**
     * 极限压缩文本
     */
    private String buildCompactText(String text) {
        if (StrUtil.isBlank(text)) {
            return truncationSuffix;
        }
        int compactLimit = Math.min(Math.max(1, chunkSize), Math.max(1, maxMessageChars / 8));
        if (text.length() <= compactLimit) {
            return text;
        }
        return text.substring(0, compactLimit) + truncationSuffix;
    }

    /**
     * 将长文本拆分为固定大小的分片
     */
    private List<String> splitIntoChunks(String text) {
        if (StrUtil.isEmpty(text)) {
            return List.of("");
        }
        List<String> chunks = new ArrayList<>((text.length() + chunkSize - 1) / chunkSize);
        for (int start = 0; start < text.length(); start += chunkSize) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
        }
        return chunks;
    }

    /**
     * 设置Redis值+自动过期
     */
    private void setValue(String key, String value) {
        if (ttlSeconds > 0) {
            stringRedisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
            return;
        }
        stringRedisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置Redis键过期时间
     */
    private void expireKey(String key) {
        if (ttlSeconds > 0) {
            stringRedisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
        }
    }

    /**
     * 计算文本需要的分片数
     */
    private int calculateChunkCount(int textLength) {
        return Math.max(1, (textLength + chunkSize - 1) / chunkSize);
    }

    /**
     * 安全转换字符串为整数
     */
    private int parseInteger(String value) {
        if (StrUtil.isBlank(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    //  Redis 键构建工具（统一管理键规则） 
    private String buildBaseKey(Object memoryId) {
        return KEY_PREFIX + memoryId;
    }

    private String buildActiveVersionKey(String baseKey) {
        return baseKey + ACTIVE_VERSION_KEY_SUFFIX;
    }

    private String buildVersionCounterKey(String baseKey) {
        return baseKey + VERSION_COUNTER_KEY_SUFFIX;
    }

    private String buildIndexKey(String baseKey, String version) {
        return baseKey + VERSION_KEY_SEGMENT + version + INDEX_KEY_SUFFIX;
    }

    private String buildChunkCountKey(String baseKey, String version, Object messageIndex) {
        return baseKey + VERSION_KEY_SEGMENT + version + MESSAGE_KEY_SEGMENT + messageIndex + CHUNK_COUNT_KEY_SUFFIX;
    }

    private String buildChunkKey(String baseKey, String version, Object messageIndex, int chunkIndex) {
        return baseKey + VERSION_KEY_SEGMENT + version + MESSAGE_KEY_SEGMENT + messageIndex + CHUNK_KEY_SEGMENT + chunkIndex;
    }
}