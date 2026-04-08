package com.rich.prompt.controller;

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
import com.rich.prompt.service.PromptTemplateService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户提示词管理 控制层
 * 提供用户提示词的增删改查接口
 *
 * @author DuRuiChi
 * @since 2026-03-12
 */
@RestController
@RequestMapping("/promptTemplate")
public class PromptTemplateController {

    @Resource
    private PromptTemplateService promptTemplateService;

    /**
     * 根据用户身份和行业获取匹配的模板列表（前端主页调用）
     *
     * @param userIdentity 用户身份
     * @param userIndustry 用户行业
     * @return 匹配的模板列表
     * @author DuRuiChi
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
     *
     * @param id 模板ID
     * @return 模板详情
     * @author DuRuiChi
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
     *
     * @param addRequest 新增请求参数
     * @param request    HTTP请求对象
     * @return 新增模板的ID
     * @author DuRuiChi
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
     *
     * @param updateRequest 更新请求参数
     * @return 是否更新成功
     * @author DuRuiChi
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
     *
     * @param deleteRequest 删除请求参数
     * @return 是否删除成功
     * @author DuRuiChi
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
     *
     * @param queryRequest 查询请求参数
     * @return 模板分页列表
     * @author DuRuiChi
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