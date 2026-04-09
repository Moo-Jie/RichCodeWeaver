package com.rich.app.controller;

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

    @Resource
    private CustomerServiceService customerServiceService;

    @PostMapping("/conversation/create")
    public BaseResponse<CustomerServiceConversationVO> createConversation(HttpServletRequest request) {
        return ResultUtils.success(customerServiceService.createConversation(request));
    }

    @GetMapping("/conversation/list")
    public BaseResponse<List<CustomerServiceConversationVO>> listConversations(HttpServletRequest request) {
        return ResultUtils.success(customerServiceService.listConversations(request));
    }

    @GetMapping("/message/list/{conversationId}")
    public BaseResponse<List<CustomerServiceMessageVO>> listMessages(@PathVariable Long conversationId,
                                                                     @RequestParam(defaultValue = "20") int pageSize,
                                                                     @RequestParam(required = false)
                                                                     @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                                                     LocalDateTime lastCreateTime,
                                                                     HttpServletRequest request) {
        ThrowUtils.throwIf(conversationId == null || conversationId <= 0, ErrorCode.PARAMS_ERROR, "会话ID无效");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 100, ErrorCode.PARAMS_ERROR, "页面大小必须在1-100之间");
        return ResultUtils.success(customerServiceService.listMessages(conversationId, pageSize, lastCreateTime, request));
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(CustomerServiceChatRequest chatRequest,
                                                    HttpServletRequest request) {
        ThrowUtils.throwIf(chatRequest == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        return customerServiceService.chatStream(chatRequest, request);
    }
}
