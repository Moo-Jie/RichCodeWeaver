package com.rich.richcodeweaver.controller;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.rich.richcodeweaver.annotation.AuthCheck;
import com.rich.richcodeweaver.constant.UserConstant;
import com.rich.richcodeweaver.exception.ErrorCode;
import com.rich.richcodeweaver.exception.ThrowUtils;
import com.rich.richcodeweaver.model.common.BaseResponse;
import com.rich.richcodeweaver.model.dto.chathistory.ChatHistoryQueryRequest;
import com.rich.richcodeweaver.model.entity.ChatHistory;
import com.rich.richcodeweaver.service.AppService;
import com.rich.richcodeweaver.service.ChatHistoryService;
import com.rich.richcodeweaver.utils.ResultUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 对话历史 控制层。
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private AppService appService;

    /**
     * 分页查询登录用户所属的某个应用的对话历史消息
     *
     * @param appId          应用ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后一条记录的创建时间
     * @param request        请求
     * @return BaseResponse<com.mybatisflex.core.paginate.Page < ChatHistory>> 对话历史分页
     * @author DuRuiChi
     * @create 2025/8/16
     **/
    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistory>> listAppChatHistoryByPage(@PathVariable Long appId,
                                                                    @RequestParam(defaultValue = "10") int pageSize,
                                                                    @RequestParam(required = false) LocalDateTime lastCreateTime,
                                                                    HttpServletRequest request) {
        Page<ChatHistory> result = chatHistoryService.listAppChatHistoryByPage(appId, pageSize, lastCreateTime, request);
        return ResultUtils.success(result);
    }

    /**
     * 分页查询所有对话历史(管理员)
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return com.rich.richcodeweaver.model.common.BaseResponse<com.mybatisflex.core.paginate.Page < com.rich.richcodeweaver.model.entity.ChatHistory>> 对话历史分页
     * @author DuRuiChi
     * @create 2025/8/16
     **/
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listAppChatHistoryByPageAdmin(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = chatHistoryQueryRequest.getPageNum();
        long pageSize = chatHistoryQueryRequest.getPageSize();
        // 查询数据
        QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(chatHistoryQueryRequest);
        Page<ChatHistory> result = chatHistoryService.page(Page.of(pageNum, pageSize), queryWrapper);
        return ResultUtils.success(result);
    }

    /**
     * 根据历史消息 id 删除对话历史(管理员)
     *
     * @param id 历史消息 id
     * @return BaseResponse<Boolean> 删除结果
     * @author DuRuiChi
     * @create 2025/8/16
     **/
    @DeleteMapping("/admin/delete/{id}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteById(@PathVariable Long id) {
        // 参数校验
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("id", id);
        // 删除对话历史
        return ResultUtils.success(chatHistoryService.remove(queryWrapper));
    }
}
