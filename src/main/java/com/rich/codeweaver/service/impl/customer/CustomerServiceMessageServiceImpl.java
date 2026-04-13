package com.rich.codeweaver.service.impl.customer;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.codeweaver.common.constant.CustomerServiceConstant;
import com.rich.codeweaver.common.exception.ErrorCode;
import com.rich.codeweaver.common.exception.ThrowUtils;
import com.rich.codeweaver.mapper.CustomerServiceMessageMapper;
import com.rich.codeweaver.model.entity.CustomerServiceMessage;
import com.rich.codeweaver.model.enums.CustomerServiceMessageRoleEnum;
import com.rich.codeweaver.model.vo.CustomerServiceMessageVO;
import com.rich.codeweaver.service.customer.CustomerServiceMessageService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 客服消息 Service 实现
 *
 * @author DuRuiChi
 * @create 2026/4/9
 */
@Service
public class CustomerServiceMessageServiceImpl extends ServiceImpl<CustomerServiceMessageMapper, CustomerServiceMessage>
        implements CustomerServiceMessageService {

    /**
     * 新增一条客服消息
     *
     * @param conversationId 会话 ID
     * @param senderType 发送方类型
     * @param content 消息内容
     * @param userId 登录用户 ID
     * @param visitorKey 游客标识
     * @return 消息实体
     */
    @Override
    public CustomerServiceMessage addMessage(Long conversationId, String senderType, String content, Long userId, String visitorKey) {
        ThrowUtils.throwIf(conversationId == null || conversationId <= 0, ErrorCode.PARAMS_ERROR, "会话ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(content), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        CustomerServiceMessageRoleEnum roleEnum = CustomerServiceMessageRoleEnum.getEnumByValue(senderType);
        ThrowUtils.throwIf(roleEnum == null, ErrorCode.PARAMS_ERROR, "消息发送方类型不合法");
        CustomerServiceMessage message = CustomerServiceMessage.builder()
                .conversationId(conversationId)
                .senderType(senderType)
                .content(content)
                .userId(userId)
                .visitorKey(visitorKey)
                .build();
        boolean saveResult = save(message);
        ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR, "保存 AI 客服消息失败");
        return message;
    }

    /**
     * 查询指定会话的历史消息
     *
     * @param conversationId 会话 ID
     * @param pageSize 分页大小
     * @param lastCreateTime 上一页最后一条消息时间
     * @return 消息列表
     */
    @Override
    public List<CustomerServiceMessageVO> listConversationMessages(Long conversationId, int pageSize,
                                                                   java.time.LocalDateTime lastCreateTime) {
        ThrowUtils.throwIf(conversationId == null || conversationId <= 0, ErrorCode.PARAMS_ERROR, "会话ID无效");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > CustomerServiceConstant.MAX_MESSAGE_PAGE_SIZE,
                ErrorCode.PARAMS_ERROR, "页面大小必须在1-100之间");
        QueryWrapper queryWrapper = QueryWrapper.create().from(CustomerServiceMessage.class);
        if (lastCreateTime != null) {
            queryWrapper.where("conversationId = ? AND createTime < ?", conversationId, lastCreateTime);
        } else {
            queryWrapper.where("conversationId = ?", conversationId);
        }
        queryWrapper.orderBy("createTime DESC").limit(pageSize);
        List<CustomerServiceMessage> records = list(queryWrapper);
        Collections.reverse(records);
        return records.stream()
                .map(item -> BeanUtil.toBean(item, CustomerServiceMessageVO.class))
                .collect(Collectors.toList());
    }

    /**
     * 将客服历史消息加载到对话记忆
     *
     * @param conversationId 会话 ID
     * @param chatMemory 对话记忆
     * @param maxCount 最大加载数量
     * @return 是否加载成功
     */
    @Override
    public Boolean loadChatHistoryToMemory(Long conversationId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            ThrowUtils.throwIf(conversationId == null || conversationId <= 0, ErrorCode.PARAMS_ERROR, "会话ID无效");
            ThrowUtils.throwIf(chatMemory == null, ErrorCode.PARAMS_ERROR, "聊天记忆对象不能为空");
            ThrowUtils.throwIf(maxCount <= 0, ErrorCode.PARAMS_ERROR, "加载数量必须大于 0");
            QueryWrapper queryWrapper = QueryWrapper.create().from(CustomerServiceMessage.class)
                    .where("conversationId = ?", conversationId)
                    .orderBy("createTime DESC")
                    .limit(maxCount);
            List<CustomerServiceMessage> messageList = list(queryWrapper);
            if (messageList == null || messageList.isEmpty()) {
                return true;
            }
            Collections.reverse(messageList);
            chatMemory.clear();
            for (CustomerServiceMessage message : messageList) {
                addMessageToChatMemory(chatMemory, message);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 按消息角色写入对话记忆
     *
     * @param chatMemory 对话记忆
     * @param message 消息实体
     */
    private void addMessageToChatMemory(MessageWindowChatMemory chatMemory, CustomerServiceMessage message) {
        if (CustomerServiceMessageRoleEnum.USER.getValue().equals(message.getSenderType())) {
            chatMemory.add(UserMessage.from(message.getContent()));
        } else if (CustomerServiceMessageRoleEnum.AI.getValue().equals(message.getSenderType())) {
            chatMemory.add(AiMessage.from(message.getContent()));
        }
    }
}
