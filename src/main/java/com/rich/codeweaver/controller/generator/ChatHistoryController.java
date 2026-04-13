package com.rich.codeweaver.controller.generator;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.rich.codeweaver.common.constant.UserConstant;
import com.rich.codeweaver.common.model.BaseResponse;
import com.rich.codeweaver.common.utils.ResultUtils;
import com.rich.codeweaver.common.exception.ErrorCode;
import com.rich.codeweaver.common.exception.ThrowUtils;
import com.rich.codeweaver.model.annotation.AuthCheck;
import com.rich.codeweaver.model.dto.chathistory.ChatHistoryQueryRequest;
import com.rich.codeweaver.model.entity.ChatHistory;
import com.rich.codeweaver.service.generator.AppService;
import com.rich.codeweaver.service.generator.ChatHistoryService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 对话历史 控制层
 * 提供对话历史的查询、删除等接口
 *
 * @author DuRuiChi
 * @since 2026-03-10
 */
@RestController
@RequestMapping("/generator/chatHistory")
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private AppService appService;

    /**
     * 分页查询登录用户所属的某个产物的对话历史消息
     *
     * @param appId          产物ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后一条记录的创建时间
     * @param request        请求
     * @return BaseResponse<com.mybatisflex.core.paginate.Page < ChatHistory>> 对话历史分页
     * @author DuRuiChi
     * @create 2025/12/16
     **/
    @GetMapping("/app/{appId}")
//    @RateLimit(type = RateLimitTypeEnum.API, rate = 30, window = 10)
    public BaseResponse<Page<ChatHistory>> listAppChatHistoryByPage(@PathVariable Long appId,
                                                                    @RequestParam(defaultValue = "10") int pageSize,
                                                                    @RequestParam(required = false) LocalDateTime lastCreateTime,
                                                                    HttpServletRequest request) {
        // 参数校验：验证产物ID有效性
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "产物ID无效");
        // 参数校验：验证分页参数合理性（限制最大100条防止数据量过大）
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 100, ErrorCode.PARAMS_ERROR, "页面大小必须在1-100之间");

        // 查询指定产物的对话历史（支持游标分页）
        Page<ChatHistory> chatHistoryPage = chatHistoryService.listAppChatHistoryByPage(
                appId, pageSize, lastCreateTime, request);
        return ResultUtils.success(chatHistoryPage);
    }

    /**
     * 分页查询所有对话历史(管理员)
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return com.rich.codeweaver.model.common.BaseResponse<com.mybatisflex.core.paginate.Page < com.rich.codeweaver.model.entity.ChatHistory>> 对话历史分页
     * @author DuRuiChi
     * @create 2025/12/16
     **/
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listAppChatHistoryByPageAdmin(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        // 参数校验：验证查询请求不为空
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR, "查询请求参数不能为空");

        // 提取分页参数
        long pageNum = chatHistoryQueryRequest.getPageNum();
        long pageSize = chatHistoryQueryRequest.getPageSize();

        // 参数校验：验证分页参数合理性
        ThrowUtils.throwIf(pageNum <= 0, ErrorCode.PARAMS_ERROR, "页码必须大于0");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 100, ErrorCode.PARAMS_ERROR, "页面大小必须在1-100之间");

        // 构建查询条件
        QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(chatHistoryQueryRequest);

        // 执行分页查询
        Page<ChatHistory> chatHistoryPage = chatHistoryService.page(
                Page.of(pageNum, pageSize), queryWrapper);
        return ResultUtils.success(chatHistoryPage);
    }

    /**
     * 根据历史消息 id 删除对话历史(管理员)
     *
     * @param id 历史消息 id
     * @return BaseResponse<Boolean> 删除结果
     * @author DuRuiChi
     * @create 2025/12/16
     **/
    @DeleteMapping("/admin/delete/{id}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteById(@PathVariable Long id) {
        // 参数校验：验证历史消息ID有效性
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "历史消息ID无效");

        // 构建删除条件：根据ID删除
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("id", id);

        // 执行删除操作
        boolean deleteResult = chatHistoryService.remove(queryWrapper);
        return ResultUtils.success(deleteResult);
    }
}
