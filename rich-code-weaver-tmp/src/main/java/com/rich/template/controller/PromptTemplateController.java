package com.rich.template.controller;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.rich.client.innerService.InnerUserService;
import com.rich.common.constant.UserConstant;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.common.model.BaseResponse;
import com.rich.common.model.DeleteRequest;
import com.rich.common.utils.ResultUtils;
import com.rich.model.annotation.AuthCheck;
import com.rich.model.dto.prompttemplate.PromptTemplateAddRequest;
import com.rich.model.dto.prompttemplate.PromptTemplateQueryRequest;
import com.rich.model.dto.prompttemplate.PromptTemplateUpdateRequest;
import com.rich.model.entity.PromptTemplate;
import com.rich.model.vo.PromptTemplateVO;
import com.rich.template.service.PromptTemplateService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 提示词模板
 */
@RestController
@RequestMapping("/promptTemplate")
public class PromptTemplateController {

    @Resource
    private PromptTemplateService promptTemplateService;

    /**
     * 根据用户身份和行业获取匹配的模板列表（前端主页调用）
     */
    @GetMapping("/list/matched")
    public BaseResponse<List<PromptTemplateVO>> listMatchedTemplates(
            @RequestParam(required = false) String userIdentity,
            @RequestParam(required = false) String userIndustry) {
        List<PromptTemplateVO> list = promptTemplateService.listMatchedTemplates(userIdentity, userIndustry);
        return ResultUtils.success(list);
    }

    /**
     * 根据id获取模板详情
     */
    @GetMapping("/get/vo")
    public BaseResponse<PromptTemplateVO> getPromptTemplateVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        PromptTemplate template = promptTemplateService.getById(id);
        ThrowUtils.throwIf(template == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(promptTemplateService.getPromptTemplateVO(template));
    }

    // ===== 管理员接口 =====

    /**
     * 新增模板（管理员）
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addPromptTemplate(@RequestBody PromptTemplateAddRequest addRequest,
                                                HttpServletRequest request) {
        ThrowUtils.throwIf(addRequest == null, ErrorCode.PARAMS_ERROR);
        PromptTemplate template = new PromptTemplate();
        BeanUtil.copyProperties(addRequest, template);
        template.setUserId(InnerUserService.getLoginUser(request).getId());
        boolean result = promptTemplateService.save(template);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(template.getId());
    }

    /**
     * 更新模板（管理员）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePromptTemplate(@RequestBody PromptTemplateUpdateRequest updateRequest) {
        if (updateRequest == null || updateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        PromptTemplate template = new PromptTemplate();
        BeanUtil.copyProperties(updateRequest, template);
        boolean result = promptTemplateService.updateById(template);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 删除模板（管理员）
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deletePromptTemplate(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = promptTemplateService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 分页查询模板（管理员）
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<PromptTemplateVO>> listPromptTemplateByPage(
            @RequestBody PromptTemplateQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = queryRequest.getPageNum();
        long pageSize = queryRequest.getPageSize();
        Page<PromptTemplate> page = promptTemplateService.page(
                Page.of(pageNum, pageSize),
                promptTemplateService.getQueryWrapper(queryRequest));
        Page<PromptTemplateVO> voPage = new Page<>(page.getRecords().stream()
                .map(promptTemplateService::getPromptTemplateVO)
                .toList(), pageNum, pageSize, page.getTotalRow());
        return ResultUtils.success(voPage);
    }
}