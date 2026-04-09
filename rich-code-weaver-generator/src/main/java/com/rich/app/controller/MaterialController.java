package com.rich.app.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.rich.app.service.MaterialCategoryService;
import com.rich.app.service.MaterialService;
import com.rich.client.innerService.InnerUserService;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.common.model.BaseResponse;
import com.rich.common.model.DeleteRequest;
import com.rich.common.utils.ResultUtils;
import com.rich.model.dto.material.MaterialAddRequest;
import com.rich.model.dto.material.MaterialQueryRequest;
import com.rich.model.dto.material.MaterialUpdateRequest;
import com.rich.model.entity.Material;
import com.rich.model.entity.MaterialCategory;
import com.rich.model.entity.User;
import com.rich.model.vo.MaterialVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.function.Consumer;

/**
 * 素材 Controller
 * 用户可管理自己的素材，也可查看公开素材
 *
 * @author DuRuiChi
 * @create 2026/3/30
 */
@RestController
@RequestMapping("/generator/material")
@Slf4j
public class MaterialController {

    @Resource
    private MaterialService materialService;

    @Resource
    private MaterialCategoryService materialCategoryService;

    @Resource
    private InnerUserService userService;

    // region 素材管理接口

    /**
     * 添加素材
     */
    @PostMapping("/add")
    public BaseResponse<Long> addMaterial(@RequestBody MaterialAddRequest addRequest,
                                          HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(addRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(addRequest.getMaterialName()), ErrorCode.PARAMS_ERROR, "素材名称不能为空");
        ThrowUtils.throwIf(addRequest.getCategoryId() == null, ErrorCode.PARAMS_ERROR, "分类不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(addRequest.getMaterialType()), ErrorCode.PARAMS_ERROR, "素材类型不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(addRequest.getContent()), ErrorCode.PARAMS_ERROR, "素材内容不能为空");

        // 获取登录用户
        User loginUser = InnerUserService.getLoginUser(request);

        // 校验分类是否存在且已启用
        MaterialCategory category = materialCategoryService.getById(addRequest.getCategoryId());
        ThrowUtils.throwIf(category == null, ErrorCode.PARAMS_ERROR, "分类不存在");
        ThrowUtils.throwIf(category.getIsEnabled() != 1, ErrorCode.PARAMS_ERROR, "分类已禁用");

        // 创建实体
        Material material = new Material();
        BeanUtil.copyProperties(addRequest, material);
        material.setUserId(loginUser.getId());
        material.setUseCount(0);

        // 设置默认值
        if (material.getIsPublic() == null) {
            material.setIsPublic(0); // 默认私有
        }

        // 保存
        boolean result = materialService.save(material);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "添加失败");

        log.info("用户 {} 添加素材: {}", loginUser.getId(), material.getMaterialName());
        return ResultUtils.success(material.getId());
    }

    /**
     * 更新素材
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateMaterial(@RequestBody MaterialUpdateRequest updateRequest,
                                                 HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(updateRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(updateRequest.getId() == null || updateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR, "素材ID无效");

        // 获取登录用户
        User loginUser = InnerUserService.getLoginUser(request);

        // 检查素材是否存在
        Material oldMaterial = materialService.getById(updateRequest.getId());
        ThrowUtils.throwIf(oldMaterial == null, ErrorCode.NOT_FOUND_ERROR, "素材不存在");

        // 权限校验：只能修改自己的素材（管理员可修改所有）
        if (!oldMaterial.getUserId().equals(loginUser.getId()) && !InnerUserService.isAdmin(loginUser)) {
            ThrowUtils.throwIf(true, ErrorCode.NO_AUTH_ERROR, "无权修改该素材");
        }

        // 如果修改了分类，校验新分类是否存在且已启用
        if (updateRequest.getCategoryId() != null && !updateRequest.getCategoryId().equals(oldMaterial.getCategoryId())) {
            MaterialCategory category = materialCategoryService.getById(updateRequest.getCategoryId());
            ThrowUtils.throwIf(category == null, ErrorCode.PARAMS_ERROR, "分类不存在");
            ThrowUtils.throwIf(category.getIsEnabled() != 1, ErrorCode.PARAMS_ERROR, "分类已禁用");
        }

        // 更新实体
        Material material = new Material();
        BeanUtil.copyProperties(updateRequest, material);

        boolean result = materialService.updateById(material);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新失败");

        log.info("用户 {} 更新素材: {}", loginUser.getId(), updateRequest.getId());
        return ResultUtils.success(true);
    }

    /**
     * 删除素材
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteMaterial(@RequestBody DeleteRequest deleteRequest,
                                                 HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(deleteRequest.getId() == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR, "素材ID无效");

        // 获取登录用户
        User loginUser = InnerUserService.getLoginUser(request);

        // 检查素材是否存在
        Material material = materialService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(material == null, ErrorCode.NOT_FOUND_ERROR, "素材不存在");

        // 权限校验：只能删除自己的素材（管理员可删除所有）
        if (!material.getUserId().equals(loginUser.getId()) && !InnerUserService.isAdmin(loginUser)) {
            ThrowUtils.throwIf(true, ErrorCode.NO_AUTH_ERROR, "无权删除该素材");
        }

        // 删除（逻辑删除）
        boolean result = materialService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除失败");

        log.info("用户 {} 删除素材: {}", loginUser.getId(), deleteRequest.getId());
        return ResultUtils.success(true);
    }

    /**
     * 根据ID获取素材详情
     */
    @GetMapping("/get/vo")
    public BaseResponse<MaterialVO> getMaterialVOById(@RequestParam Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "素材ID无效");

        Material material = materialService.getById(id);
        ThrowUtils.throwIf(material == null, ErrorCode.NOT_FOUND_ERROR, "素材不存在");

        // 获取登录用户
        User loginUser = InnerUserService.getLoginUser(request);

        // 权限校验：私有素材只能本人或管理员查看
        if (material.getIsPublic() != 1
                && !material.getUserId().equals(loginUser.getId())
                && !InnerUserService.isAdmin(loginUser)) {
            ThrowUtils.throwIf(true, ErrorCode.NO_AUTH_ERROR, "无权查看该素材");
        }

        return ResultUtils.success(materialService.getMaterialVO(material));
    }

    // endregion

    // region 素材查询接口

    /**
     * 分页查询我的素材
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<MaterialVO>> listMyMaterialByPage(
            @RequestBody MaterialQueryRequest queryRequest,
            HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);

        // 获取登录用户
        User loginUser = InnerUserService.getLoginUser(request);

        int pageNum = queryRequest.getPageNum();
        int pageSize = queryRequest.getPageSize();
        // 限制最大分页大小
        ThrowUtils.throwIf(pageSize > 50, ErrorCode.PARAMS_ERROR, "分页大小不能超过50");

        // 强制查询当前用户的素材
        queryRequest.setUserId(loginUser.getId());

        // 查询
        QueryWrapper queryWrapper = materialService.getQueryWrapper(queryRequest);
        Page<Material> page = materialService.page(new Page<>(pageNum, pageSize), queryWrapper);

        // 转换为 VO
        List<MaterialVO> voList = materialService.getMaterialVOList(page.getRecords());
        Page<MaterialVO> voPage = new Page<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow());
        voPage.setRecords(voList);

        return ResultUtils.success(voPage);
    }

    /**
     * 分页查询公开素材（用于素材选择弹窗）
     */
    @PostMapping("/public/list/page/vo")
    public BaseResponse<Page<MaterialVO>> listPublicMaterialByPage(
            @RequestBody MaterialQueryRequest queryRequest,
            HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);

        // 获取登录用户
        User loginUser = InnerUserService.getLoginUser(request);

        int pageNum = queryRequest.getPageNum();
        int pageSize = queryRequest.getPageSize();
        // 限制最大分页大小
        ThrowUtils.throwIf(pageSize > 50, ErrorCode.PARAMS_ERROR, "分页大小不能超过50");

        // 强制查询公开素材
        queryRequest.setIsPublic(1);

        // 查询
        QueryWrapper queryWrapper = materialService.getQueryWrapper(queryRequest);
        Page<Material> page = materialService.page(new Page<>(pageNum, pageSize), queryWrapper);

        // 转换为 VO
        List<MaterialVO> voList = materialService.getMaterialVOList(page.getRecords());
        Page<MaterialVO> voPage = new Page<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow());
        voPage.setRecords(voList);

        return ResultUtils.success(voPage);
    }

    /**
     * 查询可选素材列表（用于主页素材选择弹窗）
     * 返回：用户自己的素材 + 公开素材（去重）
     */
    @PostMapping("/selectable/list/page/vo")
    public BaseResponse<Page<MaterialVO>> listSelectableMaterialByPage(
            @RequestBody MaterialQueryRequest queryRequest,
            HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);

        // 获取登录用户
        User loginUser = InnerUserService.getLoginUser(request);

        int pageNum = queryRequest.getPageNum();
        int pageSize = queryRequest.getPageSize();
        // 限制最大分页大小
        ThrowUtils.throwIf(pageSize > 50, ErrorCode.PARAMS_ERROR, "分页大小不能超过50");

        // 构建基础查询条件
        QueryWrapper baseWrapper = materialService.getQueryWrapper(queryRequest);
        
        // 添加权限过滤条件：(userId = 当前用户) OR (isPublic = 1)
        baseWrapper.and((Consumer<QueryWrapper>) qw -> qw
                .eq("userId", loginUser.getId())
                .or("isPublic = 1")
        );

        // 查询
        Page<Material> page = materialService.page(new Page<>(pageNum, pageSize), baseWrapper);

        // 转换为 VO
        List<MaterialVO> voList = materialService.getMaterialVOList(page.getRecords());
        Page<MaterialVO> voPage = new Page<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow());
        voPage.setRecords(voList);

        return ResultUtils.success(voPage);
    }

    /**
     * 管理员分页查询所有素材（不限用户、不限公开）
     */
    @PostMapping("/admin/list/page/vo")
    public BaseResponse<Page<MaterialVO>> listAllMaterialByPage(
            @RequestBody MaterialQueryRequest queryRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);

        User loginUser = InnerUserService.getLoginUser(request);
        ThrowUtils.throwIf(!InnerUserService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无权访问");

        int pageNum = queryRequest.getPageNum();
        int pageSize = queryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 100, ErrorCode.PARAMS_ERROR, "分页大小不能超过100");

        QueryWrapper queryWrapper = materialService.getQueryWrapper(queryRequest);
        Page<Material> page = materialService.page(new Page<>(pageNum, pageSize), queryWrapper);

        List<MaterialVO> voList = materialService.getMaterialVOList(page.getRecords());
        Page<MaterialVO> voPage = new Page<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow());
        voPage.setRecords(voList);

        return ResultUtils.success(voPage);
    }

    /**
     * 根据ID列表批量获取素材（用于对话时获取选中的素材）
     */
    @PostMapping("/batch/get")
    public BaseResponse<List<MaterialVO>> batchGetMaterials(@RequestBody List<Long> ids,
                                                             HttpServletRequest request) {
        ThrowUtils.throwIf(CollUtil.isEmpty(ids), ErrorCode.PARAMS_ERROR, "素材ID列表不能为空");
        ThrowUtils.throwIf(ids.size() > 20, ErrorCode.PARAMS_ERROR, "一次最多选择20个素材");

        // 获取登录用户
        User loginUser = InnerUserService.getLoginUser(request);

        // 查询素材
        List<Material> materials = materialService.listByIds(ids);

        // 过滤：只返回用户有权访问的素材（自己的 + 公开的）
        List<Material> accessibleMaterials = materials.stream()
                .filter(m -> m.getUserId().equals(loginUser.getId()) || m.getIsPublic() == 1)
                .toList();

        // 转换为 VO
        List<MaterialVO> voList = materialService.getMaterialVOList(accessibleMaterials);

        return ResultUtils.success(voList);
    }

    // endregion
}
