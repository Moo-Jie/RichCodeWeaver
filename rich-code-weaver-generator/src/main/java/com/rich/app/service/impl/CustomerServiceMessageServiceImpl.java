package com.rich.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.app.mapper.CustomerServiceMessageMapper;
import com.rich.app.service.CustomerServiceMessageService;
import com.rich.common.constant.CustomerServiceConstant;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.model.entity.CustomerServiceMessage;
import com.rich.model.enums.CustomerServiceMessageRoleEnum;
import com.rich.model.vo.CustomerServiceMessageVO;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 客服消息 Service 实现
 * 提供客服消息的增删查改及对话记忆回填等业务逻辑
 *
 * @author DuRuiChi
 * @create 2026/4/9
 */
@Slf4j
@Service
public class CustomerServiceMessageServiceImpl extends ServiceImpl<CustomerServiceMessageMapper, CustomerServiceMessage>
        implements CustomerServiceMessageService {

    /**
     * 新增一条客服消息
     *
     * @param conversationId 会话 ID
     * @param senderType     发送方类型
     * @param content        消息内容
     * @param userId         登录用户 ID
     * @param visitorKey     游客标识
     * @return 消息实体
     */
    @Override
    public CustomerServiceMessage addMessage(Long conversationId, String senderType, String content, Long userId, String visitorKey) {
        // 参数校验：验证会话ID有效性
        ThrowUtils.throwIf(conversationId == null || conversationId <= 0, ErrorCode.PARAMS_ERROR, "会话ID无效");
        // 参数校验：验证消息内容不为空
        ThrowUtils.throwIf(StrUtil.isBlank(content), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        // 参数校验：验证消息发送方类型合法
        CustomerServiceMessageRoleEnum roleEnum = CustomerServiceMessageRoleEnum.getEnumByValue(senderType);
        ThrowUtils.throwIf(roleEnum == null, ErrorCode.PARAMS_ERROR, "消息发送方类型不合法");

        // 构建消息实体
        CustomerServiceMessage message = CustomerServiceMessage.builder()
                .conversationId(conversationId)
                .senderType(senderType)
                .content(content)
                .userId(userId)
                .visitorKey(visitorKey)
                .build();

        // 保存到数据库
        boolean saveResult = save(message);
        ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR, "保存 AI 客服消息失败");

        log.debug("保存客服消息成功: conversationId={}, senderType={}, contentLength={}",
                conversationId, senderType, content.length());
        return message;
    }

    /**
     * 查询指定会话的历史消息（游标分页）
     *
     * @param conversationId 会话 ID
     * @param pageSize       分页大小
     * @param lastCreateTime 上一页最后一条消息时间（游标）
     * @return 消息列表
     */
    @Override
    public List<CustomerServiceMessageVO> listConversationMessages(Long conversationId, int pageSize,
                                                                   LocalDateTime lastCreateTime) {
        // 参数校验
        ThrowUtils.throwIf(conversationId == null || conversationId <= 0, ErrorCode.PARAMS_ERROR, "会话ID无效");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > CustomerServiceConstant.MAX_MESSAGE_PAGE_SIZE,
                ErrorCode.PARAMS_ERROR, "页面大小必须在1-100之间");

        // 构建查询条件（使用 lambda 引用，保持类型安全）
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(CustomerServiceMessage::getConversationId, conversationId)
                .orderBy(CustomerServiceMessage::getCreateTime, false)
                .limit(pageSize);

        // 游标分页：查询指定时间戳之前的记录
        if (lastCreateTime != null) {
            queryWrapper.lt(CustomerServiceMessage::getCreateTime, lastCreateTime);
        }

        List<CustomerServiceMessage> records = list(queryWrapper);
        // 反转为时间正序（旧→新）
        Collections.reverse(records);
        return records.stream()
                .map(item -> BeanUtil.toBean(item, CustomerServiceMessageVO.class))
                .collect(Collectors.toList());
    }

    /**
     * 从数据库中加载客服历史消息到对话记忆中
     * 加载策略与 ChatHistoryServiceImpl.loadChatHistoryToMemory 保持对齐：
     * 1. 优先使用 Redis 中已有的对话记忆，避免重复回填
     * 2. 跳过最新1条消息（触发本次服务创建的用户消息，Langchain4j 会自动加入）
     * 3. 反转列表为时间正序后写入 chatMemory
     *
     * @param conversationId 会话 ID（客服模式以 conversationId 隔离，区别于 Agent/Workflow 的 appId）
     * @param chatMemory     对话记忆
     * @param maxCount       最大加载数量
     * @return 是否加载成功
     */
    @Override
    public Boolean loadChatHistoryToMemory(Long conversationId, ChatMemory chatMemory, int maxCount) {
        try {
            // 参数校验：确保会话ID、聊天记忆对象和加载数量有效
            if (conversationId == null || conversationId <= 0) {
                log.warn("加载客服对话历史失败：会话ID无效 - conversationId={}", conversationId);
                return false;
            }
            if (chatMemory == null) {
                log.warn("加载客服对话历史失败：聊天记忆对象为空 - conversationId={}", conversationId);
                return false;
            }
            if (maxCount <= 0) {
                log.warn("加载客服对话历史失败：加载数量无效 - conversationId={}, maxCount={}", conversationId, maxCount);
                return false;
            }

            // 优先使用 Redis 中已有的对话记忆
            // Redis 中的数据由 Langchain4j TokenWindowChatMemory 在每次对话时自动维护
            try {
                List<?> existingMessages = chatMemory.messages();
                if (existingMessages != null && !existingMessages.isEmpty()) {
                    log.info("Redis 中已存在 conversationId: {} 的 {} 条对话记忆，跳过数据库加载",
                            conversationId, existingMessages.size());
                    return true;
                }
            } catch (Exception e) {
                log.warn("检查 Redis 对话记忆时异常，将从数据库回填: conversationId={}, error={}", conversationId, e.getMessage());
            }

            // 跳过第1条（最新记录）：该记录是触发本次 AI 服务创建的用户消息，
            // 会由 Langchain4j 在 chatStream() 调用时自动加入 chatMemory，此处跳过防止重复
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(CustomerServiceMessage::getConversationId, conversationId)
                    .orderBy(CustomerServiceMessage::getCreateTime, false)
                    .limit(1, maxCount);

            List<CustomerServiceMessage> messageList = list(queryWrapper);
            if (CollUtil.isEmpty(messageList)) {
                log.info("冷启动回填：conversationId={} 无历史记录", conversationId);
                return true;
            }

            // 反转列表，转换为正序（旧→新），符合对话上下文的时间顺序
            messageList = messageList.reversed();
            log.info("冷启动回填：为 conversationId: {} 从 DB 加载 {} 条历史记录到 Redis 对话记忆",
                    conversationId, messageList.size());

            // 清理后加载，确保 Redis 中只有本次回填的历史
            chatMemory.clear();

            int loadedCount = 0;
            for (CustomerServiceMessage message : messageList) {
                if (message == null || StrUtil.isBlank(message.getContent())) {
                    log.warn("跳过无效的历史记录: conversationId={}, messageId={}", conversationId,
                            message != null ? message.getId() : "null");
                    continue;
                }

                String senderType = message.getSenderType();
                if (CustomerServiceMessageRoleEnum.USER.getValue().equals(senderType)) {
                    chatMemory.add(UserMessage.from(message.getContent()));
                    loadedCount++;
                } else if (CustomerServiceMessageRoleEnum.AI.getValue().equals(senderType)) {
                    chatMemory.add(AiMessage.from(message.getContent()));
                    loadedCount++;
                } else {
                    log.warn("跳过未知类型的历史记录: conversationId={}, senderType={}", conversationId, senderType);
                }
            }

            log.info("冷启动回填成功: conversationId={}, loadedCount={}", conversationId, loadedCount);
            return true;
        } catch (Exception e) {
            log.error("【AI 客服服务实例缓存】加载对话历史失败，conversationId: {}，错误: {}", conversationId, e.getMessage(), e);
            return false;
        }
    }
}
