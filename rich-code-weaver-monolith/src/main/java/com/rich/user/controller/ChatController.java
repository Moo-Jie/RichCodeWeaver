package com.rich.user.controller;

import com.mybatisflex.core.paginate.Page;
import com.rich.common.constant.ChatConstant;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.common.model.BaseResponse;
import com.rich.common.utils.ResultUtils;
import com.rich.model.dto.chat.ChatMessageQueryRequest;
import com.rich.model.dto.chat.ChatMessageSendRequest;
import com.rich.model.entity.User;
import com.rich.model.vo.ChatConversationVO;
import com.rich.model.vo.ChatMessageVO;
import com.rich.user.service.ChatConversationService;
import com.rich.user.service.ChatMessageService;
import com.rich.user.service.UserFriendshipService;
import com.rich.user.service.UserService;
import com.rich.user.websocket.ChatWebSocketHandler;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天控制器。
 * 提供会话列表、消息查询、HTTP 降级发送、已读标记等能力。
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@RestController
@RequestMapping("/user/chat")
public class ChatController {

    private static final int MAX_MESSAGE_LENGTH = 5000;

    @Resource
    private ChatConversationService chatConversationService;

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private UserService userService;

    @Resource
    private UserFriendshipService userFriendshipService;

    @Resource
    private ChatWebSocketHandler chatWebSocketHandler;

    @GetMapping("/conversations")
    public BaseResponse<List<ChatConversationVO>> listConversations(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(chatConversationService.listConversations(loginUser.getId()));
    }

    @PostMapping("/messages")
    public BaseResponse<Page<ChatMessageVO>> listMessages(@RequestBody ChatMessageQueryRequest queryRequest,
                                                          HttpServletRequest request) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(queryRequest.getConversationId() == null || queryRequest.getConversationId() <= 0,
                ErrorCode.PARAMS_ERROR, "会话id无效");
        ThrowUtils.throwIf(queryRequest.getPageNum() < 1 || queryRequest.getPageNum() > 10000,
                ErrorCode.PARAMS_ERROR, "页码超出允许范围");
        ThrowUtils.throwIf(queryRequest.getPageSize() < 1 || queryRequest.getPageSize() > 100,
                ErrorCode.PARAMS_ERROR, "每页消息数必须在 1-100 之间");
        User loginUser = userService.getLoginUser(request);
        Page<ChatMessageVO> messages = chatMessageService.listMessages(
                queryRequest.getConversationId(), loginUser.getId(),
                queryRequest.getPageNum(), queryRequest.getPageSize());
        return ResultUtils.success(messages);
    }

    @PostMapping("/send")
    public BaseResponse<ChatMessageVO> sendMessage(@RequestBody ChatMessageSendRequest sendRequest,
                                                   HttpServletRequest request) {
        ThrowUtils.throwIf(sendRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(sendRequest.getReceiverId() == null || sendRequest.getReceiverId() <= 0,
                ErrorCode.PARAMS_ERROR, "接收方用户id不能为空");
        String content = sendRequest.getContent();
        ThrowUtils.throwIf(content == null || content.trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        ThrowUtils.throwIf(content.trim().length() > MAX_MESSAGE_LENGTH,
                ErrorCode.PARAMS_ERROR, "消息内容不能超过 5000 个字符");
        String messageType = sendRequest.getMessageType();
        if (messageType == null || messageType.isBlank()) {
            messageType = ChatConstant.MESSAGE_TYPE_TEXT;
        }
        ThrowUtils.throwIf(!ChatConstant.MESSAGE_TYPE_TEXT.equals(messageType),
                ErrorCode.PARAMS_ERROR, "普通聊天仅支持文本消息");

        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!userFriendshipService.isFriend(loginUser.getId(), sendRequest.getReceiverId()),
                ErrorCode.NO_AUTH_ERROR, "对方不是您的好友，无法发送消息");
        ChatMessageVO messageVO = chatMessageService.sendMessage(
                loginUser.getId(), sendRequest.getReceiverId(), content, messageType);
        // HTTP 是 WebSocket 的降级通道，成功后仍需要补齐实时推送与多端同步。
        chatWebSocketHandler.pushChatMessage(loginUser.getId(), sendRequest.getReceiverId(), messageVO);
        return ResultUtils.success(messageVO);
    }

    @PostMapping("/read")
    public BaseResponse<Boolean> markAsRead(@RequestParam Long conversationId,
                                            @RequestParam(required = false) Long lastReadMessageId,
                                            HttpServletRequest request) {
        ThrowUtils.throwIf(conversationId == null || conversationId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        chatMessageService.markAsRead(conversationId, loginUser.getId(), lastReadMessageId);
        chatWebSocketHandler.pushUnreadCount(loginUser.getId());
        return ResultUtils.success(true);
    }

    @GetMapping("/unread/count")
    public BaseResponse<Integer> getUnreadCount(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(chatMessageService.getUnreadCount(loginUser.getId()));
    }
}
