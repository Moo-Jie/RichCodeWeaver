package com.rich.codeweaver.controller.prompt;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;

import com.rich.codeweaver.common.constant.UserConstant;
import com.rich.codeweaver.common.model.BaseResponse;
import com.rich.codeweaver.common.utils.ResultUtils;
import com.rich.codeweaver.common.exception.ErrorCode;
import com.rich.codeweaver.common.exception.ThrowUtils;
import com.rich.codeweaver.common.model.DeleteRequest;
import com.rich.codeweaver.model.annotation.AuthCheck;
import com.rich.codeweaver.model.dto.systemPrompt.SystemPromptAddRequest;
import com.rich.codeweaver.model.dto.systemPrompt.SystemPromptQueryRequest;
import com.rich.codeweaver.model.dto.systemPrompt.SystemPromptUpdateRequest;
import com.rich.codeweaver.model.entity.SystemPrompt;
import com.rich.codeweaver.model.entity.User;
import com.rich.codeweaver.model.vo.SystemPromptVO;
import com.rich.codeweaver.service.prompt.SystemPromptService;
import com.rich.codeweaver.service.user.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统提示词管理 控制层
 * 提供系统提示词的增删改查接口
 *
 * @author DuRuiChi
 */
@Slf4j
@RestController
@RequestMapping("/prompt/systemPrompt")
public class SystemPromptController {

    @Resource
    private UserService userService;

    @Resource
    private SystemPromptService promptService;

    /**
     * 新增系统提示词（管理员）
     *
     * @param addRequest 新增请求
     * @param request    请求对象
     * @return 新记录ID
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addSystemPrompt(@RequestBody SystemPromptAddRequest addRequest,
                                              HttpServletRequest request) {
        ThrowUtils.throwIf(addRequest == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(addRequest.getPromptName()), ErrorCode.PARAMS_ERROR, "提示词名称不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(addRequest.getPromptKey()), ErrorCode.PARAMS_ERROR, "提示词标识不能为空");

        User loginUser = userService.getLoginUser(request);

        SystemPrompt systemPrompt = new SystemPrompt();
        BeanUtil.copyProperties(addRequest, systemPrompt);
        systemPrompt.setUserId(loginUser.getId());

        boolean saved = promptService.save(systemPrompt);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "新增系统提示词失败");

        log.info("新增系统提示词: id={}, promptName={}, promptKey={}",
                systemPrompt.getId(), systemPrompt.getPromptName(), systemPrompt.getPromptKey());
        return ResultUtils.success(systemPrompt.getId());
    }

    /**
     * 更新系统提示词（管理员）
     *
     * @param updateRequest 更新请求
     * @return 是否更新成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSystemPrompt(@RequestBody SystemPromptUpdateRequest updateRequest) {
        ThrowUtils.throwIf(updateRequest == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        ThrowUtils.throwIf(updateRequest.getId() == null || updateRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR, "ID无效");

        SystemPrompt existing = promptService.getById(updateRequest.getId());
        ThrowUtils.throwIf(existing == null, ErrorCode.NOT_FOUND_ERROR, "系统提示词不存在");

        SystemPrompt updateEntity = new SystemPrompt();
        BeanUtil.copyProperties(updateRequest, updateEntity);

        boolean updated = promptService.updateById(updateEntity);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新系统提示词失败");

        log.info("更新系统提示词: id={}", updateRequest.getId());
        return ResultUtils.success(true);
    }

    /**
     * 删除系统提示词（管理员，逻辑删除）
     *
     * @param deleteRequest 删除请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteSystemPrompt(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        ThrowUtils.throwIf(deleteRequest.getId() == null || deleteRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR, "ID无效");

        SystemPrompt existing = promptService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(existing == null, ErrorCode.NOT_FOUND_ERROR, "系统提示词不存在");

        boolean removed = promptService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!removed, ErrorCode.OPERATION_ERROR, "删除系统提示词失败");

        log.info("删除系统提示词: id={}, promptName={}", deleteRequest.getId(), existing.getPromptName());
        return ResultUtils.success(true);
    }

    /**
     * 根据ID获取系统提示词详情（管理员）
     *
     * @param id 提示词ID
     * @return 系统提示词视图对象
     */
    @GetMapping("/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<SystemPromptVO> getSystemPromptVOById(@RequestParam("id") long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR, "ID无效");

        SystemPrompt systemPrompt = promptService.getById(id);
        ThrowUtils.throwIf(systemPrompt == null, ErrorCode.NOT_FOUND_ERROR, "系统提示词不存在");

        return ResultUtils.success(promptService.getSystemPromptVO(systemPrompt));
    }

    /**
     * 分页查询系统提示词（管理员）
     *
     * @param queryRequest 查询请求
     * @return 分页结果
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<SystemPromptVO>> listSystemPromptByPage(@RequestBody SystemPromptQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR, "查询请求参数不能为空");

        long pageNum = queryRequest.getPageNum();
        long pageSize = queryRequest.getPageSize();
        ThrowUtils.throwIf(pageNum <= 0, ErrorCode.PARAMS_ERROR, "页码必须大于0");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 100, ErrorCode.PARAMS_ERROR, "页面大小必须在1-100之间");

        QueryWrapper queryWrapper = promptService.getQueryWrapper(queryRequest);
        Page<SystemPrompt> page = promptService.page(Page.of(pageNum, pageSize), queryWrapper);

        // 转换为VO分页
        Page<SystemPromptVO> voPage = new Page<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow());
        voPage.setRecords(promptService.getSystemPromptVOList(page.getRecords()));

        return ResultUtils.success(voPage);
    }

    /**
     * 获取所有系统提示词列表（管理员）
     *
     * @return 系统提示词列表
     */
    @GetMapping("/list/all")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<SystemPromptVO>> listAllSystemPrompts() {
        List<SystemPrompt> list = promptService.list();
        return ResultUtils.success(promptService.getSystemPromptVOList(list));
    }
}
