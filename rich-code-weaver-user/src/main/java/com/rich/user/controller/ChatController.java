package com.rich.user.controller;

import com.mybatisflex.core.paginate.Page;
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
import com.rich.user.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天控制器
 * 提供会话列表、消息查询、消息发送（HTTP方式备用）、已读标记等接口
 * 实时消息通过WebSocket推送，此控制器提供REST API作为补充
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    @Resource
    private ChatConversationService chatConversationService;

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private UserService userService;

    /**
     * 获取当前用户的会话列表
     *
     * @param request HTTP请求对象
     * @return 会话列表VO（含对方用户信息、最后消息预览、未读数）
     */
    @GetMapping("/conversations")
    public BaseResponse<List<ChatConversationVO>> listConversations(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<ChatConversationVO> conversations = chatConversationService.listConversations(loginUser.getId());
        return ResultUtils.success(conversations);
    }

    /**
     * 分页查询会话消息
     *
     * @param queryRequest 分页查询请求 { conversationId, pageNum, pageSize }
     * @param request      HTTP请求对象
     * @return 消息分页数据
     */
    @PostMapping("/messages")
    public BaseResponse<Page<ChatMessageVO>> listMessages(@RequestBody ChatMessageQueryRequest queryRequest,
                                                          HttpServletRequest request) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(queryRequest.getConversationId() == null, ErrorCode.PARAMS_ERROR, "会话id不能为空");
        // 验证用户已登录
        User loginUser = userService.getLoginUser(request);
        Page<ChatMessageVO> messages = chatMessageService.listMessages(
                queryRequest.getConversationId(), loginUser.getId(), queryRequest.getPageNum(), queryRequest.getPageSize());
        return ResultUtils.success(messages);
    }

    /**
     * 通过HTTP发送消息（WebSocket不可用时的备用方案）
     *
     * @param sendRequest 发送消息请求 { receiverId, content, messageType }
     * @param request     HTTP请求对象
     * @return 发送的消息VO
     */
    @PostMapping("/send")
    public BaseResponse<ChatMessageVO> sendMessage(@RequestBody ChatMessageSendRequest sendRequest,
                                                   HttpServletRequest request) {
        ThrowUtils.throwIf(sendRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(sendRequest.getReceiverId() == null, ErrorCode.PARAMS_ERROR, "接收方用户id不能为空");
        User loginUser = userService.getLoginUser(request);
        ChatMessageVO messageVO = chatMessageService.sendMessage(
                loginUser.getId(), sendRequest.getReceiverId(),
                sendRequest.getContent(), sendRequest.getMessageType());
        return ResultUtils.success(messageVO);
    }

    /**
     * 标记会话消息为已读
     *
     * @param conversationId 会话id
     * @param request        HTTP请求对象
     * @return 操作是否成功
     */
    @PostMapping("/read")
    public BaseResponse<Boolean> markAsRead(@RequestParam Long conversationId, HttpServletRequest request) {
        ThrowUtils.throwIf(conversationId == null || conversationId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        chatMessageService.markAsRead(conversationId, loginUser.getId());
        return ResultUtils.success(true);
    }

    /**
     * 获取当前用户的总未读消息数
     *
     * @param request HTTP请求对象
     * @return 未读消息总数
     */
    @GetMapping("/unread/count")
    public BaseResponse<Integer> getUnreadCount(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        int count = chatMessageService.getUnreadCount(loginUser.getId());
        return ResultUtils.success(count);
    }
}
