package com.rich.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.app.mapper.CustomerServiceConversationMapper;
import com.rich.app.service.CustomerServiceConversationService;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.model.entity.CustomerServiceConversation;
import com.rich.model.vo.CustomerServiceConversationVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 客服会话 Service 实现
 *
 * @author DuRuiChi
 * @create 2026/4/9
 */
@Service
public class CustomerServiceConversationServiceImpl extends ServiceImpl<CustomerServiceConversationMapper, CustomerServiceConversation>
        implements CustomerServiceConversationService {

    private static final String DEFAULT_TITLE = "新对话";

    @Override
    public CustomerServiceConversation createConversation(Long userId, String visitorKey) {
        ThrowUtils.throwIf(userId == null && StrUtil.isBlank(visitorKey), ErrorCode.PARAMS_ERROR, "访问者标识不能为空");
        CustomerServiceConversation conversation = CustomerServiceConversation.builder()
                .title(DEFAULT_TITLE)
                .userId(userId)
                .visitorKey(visitorKey)
                .lastMessagePreview(null)
                .lastMessageTime(LocalDateTime.now())
                .build();
        boolean saveResult = save(conversation);
        ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR, "创建 AI 客服会话失败");
        return conversation;
    }

    @Override
    public CustomerServiceConversation verifyAndGetConversation(Long conversationId, Long userId, String visitorKey) {
        ThrowUtils.throwIf(conversationId == null || conversationId <= 0, ErrorCode.PARAMS_ERROR, "会话ID无效");
        CustomerServiceConversation conversation = getById(conversationId);
        ThrowUtils.throwIf(conversation == null, ErrorCode.NOT_FOUND_ERROR, "会话不存在");
        boolean accessible = userId != null
                ? userId.equals(conversation.getUserId())
                : StrUtil.equals(visitorKey, conversation.getVisitorKey()) && conversation.getUserId() == null;
        ThrowUtils.throwIf(!accessible, ErrorCode.NO_AUTH_ERROR, "无权访问该客服会话");
        return conversation;
    }

    @Override
    public List<CustomerServiceConversationVO> listUserConversations(Long userId, String visitorKey) {
        QueryWrapper queryWrapper = QueryWrapper.create().from(CustomerServiceConversation.class);
        if (userId != null) {
            queryWrapper.where("userId = ?", userId);
        } else {
            queryWrapper.where("visitorKey = ? AND userId IS NULL", visitorKey);
        }
        queryWrapper.orderBy("lastMessageTime DESC, updateTime DESC");
        return list(queryWrapper).stream().map(item -> BeanUtil.toBean(item, CustomerServiceConversationVO.class))
                .collect(Collectors.toList());
    }

    @Override
    public void refreshConversation(Long conversationId, String preview, LocalDateTime messageTime, boolean updateTitle) {
        ThrowUtils.throwIf(conversationId == null || conversationId <= 0, ErrorCode.PARAMS_ERROR, "会话ID无效");
        CustomerServiceConversation conversation = getById(conversationId);
        ThrowUtils.throwIf(conversation == null, ErrorCode.NOT_FOUND_ERROR, "会话不存在");
        String safePreview = StrUtil.maxLength(StrUtil.blankToDefault(preview, ""), 120);
        conversation.setLastMessagePreview(safePreview);
        conversation.setLastMessageTime(messageTime);
        if (updateTitle && (conversation.getTitle() == null || DEFAULT_TITLE.equals(conversation.getTitle()))) {
            conversation.setTitle(StrUtil.maxLength(StrUtil.blankToDefault(preview, DEFAULT_TITLE), 24));
        }
        boolean updateResult = updateById(conversation);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新 AI 客服会话失败");
    }
}
