package com.rich.app.controller;

import com.rich.common.constant.CustomerServiceConstant;
import com.rich.app.service.CustomerServiceService;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.common.model.BaseResponse;
import com.rich.common.utils.ResultUtils;
import com.rich.model.dto.customerservice.CustomerServiceChatRequest;
import com.rich.model.vo.CustomerServiceConversationVO;
import com.rich.model.vo.CustomerServiceMessageVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 客服控制器
 *
 * @author DuRuiChi
 * @create 2026/4/9
 */
@RestController
@RequestMapping("/generator/customerService")
public class CustomerServiceController {

    /**
     * 默认消息分页大小
     */
    private static final String DEFAULT_PAGE_SIZE = "20";

    @Resource
    private CustomerServiceService customerServiceService;

    /**
     * 创建新的客服会话
     *
     * @param request 请求对象
     * @return 会话信息
     */
    @PostMapping("/conversation/create")
    public BaseResponse<CustomerServiceConversationVO> createConversation(HttpServletRequest request) {
        return ResultUtils.success(customerServiceService.createConversation(request));
    }

    /**
     * 查询当前用户的客服会话列表
     *
     * @param request 请求对象
     * @return 会话列表
     */
    @GetMapping("/conversation/list")
    public BaseResponse<List<CustomerServiceConversationVO>> listConversations(HttpServletRequest request) {
        return ResultUtils.success(customerServiceService.listConversations(request));
    }

    /**
     * 查询指定会话的历史消息
     *
     * @param conversationId 会话 ID
     * @param pageSize 分页大小
     * @param lastCreateTime 上一页最后一条消息时间
     * @param request 请求对象
     * @return 消息列表
     */
    @GetMapping("/message/list/{conversationId}")
    public BaseResponse<List<CustomerServiceMessageVO>> listMessages(@PathVariable Long conversationId,
                                                                     @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                                                     @RequestParam(required = false)
                                                                     @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                                                     LocalDateTime lastCreateTime,
                                                                     HttpServletRequest request) {
        ThrowUtils.throwIf(conversationId == null || conversationId <= 0, ErrorCode.PARAMS_ERROR, "会话ID无效");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > CustomerServiceConstant.MAX_MESSAGE_PAGE_SIZE,
                ErrorCode.PARAMS_ERROR, "页面大小必须在1-100之间");
        return ResultUtils.success(customerServiceService.listMessages(conversationId, pageSize, lastCreateTime, request));
    }

    /**
     * 发起 AI 客服流式对话
     *
     * @param chatRequest 对话请求
     * @param request 请求对象
     * @return SSE 事件流
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(CustomerServiceChatRequest chatRequest,
                                                    HttpServletRequest request) {
        ThrowUtils.throwIf(chatRequest == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        return customerServiceService.chatStream(chatRequest, request);
    }
}
