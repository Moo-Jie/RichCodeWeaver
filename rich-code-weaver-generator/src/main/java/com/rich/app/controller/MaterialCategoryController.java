package com.rich.app.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.rich.app.service.MaterialCategoryService;
import com.rich.client.innerService.InnerUserService;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.common.model.BaseResponse;
import com.rich.common.model.DeleteRequest;
import com.rich.common.utils.ResultUtils;
import com.rich.model.dto.materialcategory.MaterialCategoryAddRequest;
import com.rich.model.dto.materialcategory.MaterialCategoryQueryRequest;
import com.rich.model.dto.materialcategory.MaterialCategoryUpdateRequest;
import com.rich.model.entity.MaterialCategory;
import com.rich.model.entity.User;
import com.rich.model.vo.MaterialCategoryVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 素材分类 Controller
 * 管理员可进行增删改查，普通用户只能查询已启用的分类
 *
 * @author DuRuiChi
 * @create 2026/3/30
 */
@RestController
@RequestMapping("/generator/material/category")
@Slf4j
public class MaterialCategoryController {

    @Resource
    private MaterialCategoryService materialCategoryService;

    @Resource
    private InnerUserService userService;

    // region 管理员接口

    /**
     * 添加素材分类（管理员）
     */
    @PostMapping("/add")
    public BaseResponse<Long> addMaterialCategory(@RequestBody MaterialCategoryAddRequest addRequest,
                                                   HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(addRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(addRequest.getCategoryName()), ErrorCode.PARAMS_ERROR, "分类名称不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(addRequest.getCategoryCode()), ErrorCode.PARAMS_ERROR, "分类编码不能为空");

        // 权限校验：仅管理员可操作
        User loginUser = InnerUserService.getLoginUser(request);
        ThrowUtils.throwIf(!InnerUserService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "仅管理员可添加分类");

        // 检查分类编码是否已存在
        MaterialCategory existCategory = materialCategoryService.getByCategoryCode(addRequest.getCategoryCode());
        ThrowUtils.throwIf(existCategory != null, ErrorCode.PARAMS_ERROR, "分类编码已存在");

        // 创建实体
        MaterialCategory materialCategory = new MaterialCategory();
        BeanUtil.copyProperties(addRequest, materialCategory);
        materialCategory.setUserId(loginUser.getId());

        // 设置默认值
        if (materialCategory.getSortOrder() == null) {
            materialCategory.setSortOrder(0);
        }
        if (materialCategory.getIsEnabled() == null) {
            materialCategory.setIsEnabled(1);
        }

        // 保存
        boolean result = materialCategoryService.save(materialCategory);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "添加失败");

        log.info("管理员 {} 添加素材分类: {}", loginUser.getId(), materialCategory.getCategoryName());
        return ResultUtils.success(materialCategory.getId());
    }

    /**
     * 更新素材分类（管理员）
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateMaterialCategory(@RequestBody MaterialCategoryUpdateRequest updateRequest,
                                                         HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(updateRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(updateRequest.getId() == null || updateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR, "分类ID无效");

        // 权限校验：仅管理员可操作
        User loginUser = InnerUserService.getLoginUser(request);
        ThrowUtils.throwIf(!InnerUserService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "仅管理员可修改分类");

        // 检查分类是否存在
        MaterialCategory oldCategory = materialCategoryService.getById(updateRequest.getId());
        ThrowUtils.throwIf(oldCategory == null, ErrorCode.NOT_FOUND_ERROR, "分类不存在");

        // 如果修改了分类编码，检查新编码是否已存在
        if (StrUtil.isNotBlank(updateRequest.getCategoryCode())
                && !updateRequest.getCategoryCode().equals(oldCategory.getCategoryCode())) {
            MaterialCategory existCategory = materialCategoryService.getByCategoryCode(updateRequest.getCategoryCode());
            ThrowUtils.throwIf(existCategory != null, ErrorCode.PARAMS_ERROR, "分类编码已存在");
        }

        // 更新实体
        MaterialCategory materialCategory = new MaterialCategory();
        BeanUtil.copyProperties(updateRequest, materialCategory);

        boolean result = materialCategoryService.updateById(materialCategory);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新失败");

        log.info("管理员 {} 更新素材分类: {}", loginUser.getId(), updateRequest.getId());
        return ResultUtils.success(true);
    }

    /**
     * 删除素材分类（管理员）
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteMaterialCategory(@RequestBody DeleteRequest deleteRequest,
                                                         HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(deleteRequest.getId() == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR, "分类ID无效");

        // 权限校验：仅管理员可操作
        User loginUser = InnerUserService.getLoginUser(request);
        ThrowUtils.throwIf(!InnerUserService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "仅管理员可删除分类");

        // 检查分类是否存在
        MaterialCategory category = materialCategoryService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(category == null, ErrorCode.NOT_FOUND_ERROR, "分类不存在");

        // 删除（逻辑删除）
        boolean result = materialCategoryService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除失败");

        log.info("管理员 {} 删除素材分类: {}", loginUser.getId(), deleteRequest.getId());
        return ResultUtils.success(true);
    }

    /**
     * 分页查询素材分类（管理员）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<MaterialCategoryVO>> listMaterialCategoryByPage(
            @RequestBody MaterialCategoryQueryRequest queryRequest,
            HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);

        // 权限校验：仅管理员可操作
        User loginUser = InnerUserService.getLoginUser(request);
        ThrowUtils.throwIf(!InnerUserService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "仅管理员可查看分类列表");

        int pageNum = queryRequest.getPageNum();
        int pageSize = queryRequest.getPageSize();
        // 限制最大分页大小
        ThrowUtils.throwIf(pageSize > 50, ErrorCode.PARAMS_ERROR, "分页大小不能超过50");

        // 查询
        QueryWrapper queryWrapper = materialCategoryService.getQueryWrapper(queryRequest);
        Page<MaterialCategory> page = materialCategoryService.page(new Page<>(pageNum, pageSize), queryWrapper);

        // 转换为 VO
        List<MaterialCategoryVO> voList = materialCategoryService.getMaterialCategoryVOList(page.getRecords());
        Page<MaterialCategoryVO> voPage = new Page<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow());
        voPage.setRecords(voList);

        return ResultUtils.success(voPage);
    }

    // endregion

    // region 公共接口

    /**
     * 获取所有已启用的分类列表（公共）
     */
    @GetMapping("/list/enabled")
    public BaseResponse<List<MaterialCategoryVO>> listEnabledCategories() {
        List<MaterialCategory> categories = materialCategoryService.listEnabledCategories();
        List<MaterialCategoryVO> voList = materialCategoryService.getMaterialCategoryVOList(categories);
        return ResultUtils.success(voList);
    }

    /**
     * 根据ID获取分类详情
     */
    @GetMapping("/get/vo")
    public BaseResponse<MaterialCategoryVO> getMaterialCategoryVOById(@RequestParam Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "分类ID无效");

        MaterialCategory category = materialCategoryService.getById(id);
        ThrowUtils.throwIf(category == null, ErrorCode.NOT_FOUND_ERROR, "分类不存在");

        return ResultUtils.success(materialCategoryService.getMaterialCategoryVO(category));
    }

    // endregion
}
