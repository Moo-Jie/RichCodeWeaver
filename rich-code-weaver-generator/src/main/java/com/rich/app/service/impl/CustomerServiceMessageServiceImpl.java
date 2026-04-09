package com.rich.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.app.mapper.CustomerServiceMessageMapper;
import com.rich.app.service.CustomerServiceMessageService;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.model.entity.CustomerServiceMessage;
import com.rich.model.enums.CustomerServiceMessageRoleEnum;
import com.rich.model.vo.CustomerServiceMessageVO;
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

    @Override
    public List<CustomerServiceMessageVO> listConversationMessages(Long conversationId, int pageSize,
                                                                   java.time.LocalDateTime lastCreateTime) {
        ThrowUtils.throwIf(conversationId == null || conversationId <= 0, ErrorCode.PARAMS_ERROR, "会话ID无效");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 100, ErrorCode.PARAMS_ERROR, "页面大小必须在1-100之间");
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
                if (CustomerServiceMessageRoleEnum.USER.getValue().equals(message.getSenderType())) {
                    chatMemory.add(UserMessage.from(message.getContent()));
                } else if (CustomerServiceMessageRoleEnum.AI.getValue().equals(message.getSenderType())) {
                    chatMemory.add(AiMessage.from(message.getContent()));
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
