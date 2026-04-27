package com.rich.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.app.mapper.CustomerServiceConversationMapper;
import com.rich.app.service.CustomerServiceConversationService;
import com.rich.common.constant.CustomerServiceConstant;
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

    /**
     * 创建会话记录
     *
     * @param userId 登录用户 ID
     * @param visitorKey 游客标识
     * @return 会话实体
     */
    @Override
    public CustomerServiceConversation createConversation(Long userId, String visitorKey) {
        ThrowUtils.throwIf(userId == null && StrUtil.isBlank(visitorKey), ErrorCode.PARAMS_ERROR, "访问者标识不能为空");
        CustomerServiceConversation conversation = CustomerServiceConversation.builder()
                .title(CustomerServiceConstant.DEFAULT_CONVERSATION_TITLE)
                .userId(userId)
                .visitorKey(visitorKey)
                .lastMessagePreview(null)
                .lastMessageTime(LocalDateTime.now())
                .build();
        boolean saveResult = save(conversation);
        ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR, "创建 AI 客服会话失败");
        return conversation;
    }

    /**
     * 校验会话归属并返回会话
     *
     * @param conversationId 会话 ID
     * @param userId 登录用户 ID
     * @param visitorKey 游客标识
     * @return 会话实体
     */
    @Override
    public CustomerServiceConversation verifyAndGetConversation(Long conversationId, Long userId, String visitorKey) {
        ThrowUtils.throwIf(conversationId == null || conversationId <= 0, ErrorCode.PARAMS_ERROR, "会话ID无效");
        CustomerServiceConversation conversation = getById(conversationId);
        ThrowUtils.throwIf(conversation == null, ErrorCode.NOT_FOUND_ERROR, "会话不存在");
        boolean accessible = isConversationAccessible(conversation, userId, visitorKey);
        ThrowUtils.throwIf(!accessible, ErrorCode.NO_AUTH_ERROR, "无权访问该客服会话");
        return conversation;
    }

    /**
     * 查询用户可见的客服会话列表
     *
     * @param userId 登录用户 ID
     * @param visitorKey 游客标识
     * @return 会话列表
     */
    @Override
    public List<CustomerServiceConversationVO> listUserConversations(Long userId, String visitorKey) {
        QueryWrapper queryWrapper = QueryWrapper.create().from(CustomerServiceConversation.class);
        if (userId != null) {
            queryWrapper.where("userId = ?", userId);
        } else {
            queryWrapper.where("visitorKey = ? AND userId IS NULL", visitorKey);
        }
        queryWrapper.orderBy("lastMessageTime DESC, updateTime DESC");
        return list(queryWrapper).stream().map(this::toConversationVO)
                .collect(Collectors.toList());
    }

    /**
     * 刷新会话摘要和展示信息
     *
     * @param conversationId 会话 ID
     * @param preview 最新消息摘要
     * @param messageTime 最新消息时间
     * @param updateTitle 是否同步更新标题
     */
    @Override
    public void refreshConversation(Long conversationId, String preview, LocalDateTime messageTime, boolean updateTitle) {
        ThrowUtils.throwIf(conversationId == null || conversationId <= 0, ErrorCode.PARAMS_ERROR, "会话ID无效");
        CustomerServiceConversation conversation = getById(conversationId);
        ThrowUtils.throwIf(conversation == null, ErrorCode.NOT_FOUND_ERROR, "会话不存在");
        String safePreview = StrUtil.maxLength(StrUtil.blankToDefault(preview, ""), CustomerServiceConstant.MAX_PREVIEW_LENGTH);
        conversation.setLastMessagePreview(safePreview);
        conversation.setLastMessageTime(messageTime);
        if (updateTitle && shouldRefreshTitle(conversation)) {
            conversation.setTitle(StrUtil.maxLength(
                    StrUtil.blankToDefault(preview, CustomerServiceConstant.DEFAULT_CONVERSATION_TITLE),
                    CustomerServiceConstant.MAX_TITLE_LENGTH));
        }
        boolean updateResult = updateById(conversation);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新 AI 客服会话失败");
    }

    /**
     * 判断当前访问者是否有权访问会话
     *
     * @param conversation 会话实体
     * @param userId 登录用户 ID
     * @param visitorKey 游客标识
     * @return 是否可访问
     */
    private boolean isConversationAccessible(CustomerServiceConversation conversation, Long userId, String visitorKey) {
        return userId != null
                ? userId.equals(conversation.getUserId())
                : StrUtil.equals(visitorKey, conversation.getVisitorKey()) && conversation.getUserId() == null;
    }

    /**
     * 判断是否需要刷新默认标题
     *
     * @param conversation 会话实体
     * @return 是否刷新标题
     */
    private boolean shouldRefreshTitle(CustomerServiceConversation conversation) {
        return conversation.getTitle() == null
                || CustomerServiceConstant.DEFAULT_CONVERSATION_TITLE.equals(conversation.getTitle());
    }

    /**
     * 转换会话实体为视图对象
     *
     * @param conversation 会话实体
     * @return 会话视图对象
     */
    private CustomerServiceConversationVO toConversationVO(CustomerServiceConversation conversation) {
        return BeanUtil.toBean(conversation, CustomerServiceConversationVO.class);
    }
}
