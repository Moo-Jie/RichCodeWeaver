package com.rich.app.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.rich.model.dto.materialcategory.MaterialCategoryQueryRequest;
import com.rich.model.entity.MaterialCategory;
import com.rich.model.vo.MaterialCategoryVO;

import java.util.List;

/**
 * 素材分类 Service
 *
 * @author DuRuiChi
 * @create 2026/3/30
 */
public interface MaterialCategoryService extends IService<MaterialCategory> {

    /**
     * 构建查询条件
     *
     * @param queryRequest 查询请求
     * @return QueryWrapper
     */
    QueryWrapper getQueryWrapper(MaterialCategoryQueryRequest queryRequest);

    /**
     * 将实体转换为 VO
     *
     * @param materialCategory 实体
     * @return VO
     */
    MaterialCategoryVO getMaterialCategoryVO(MaterialCategory materialCategory);

    /**
     * 批量转换为 VO 列表
     *
     * @param list 实体列表
     * @return VO 列表
     */
    List<MaterialCategoryVO> getMaterialCategoryVOList(List<MaterialCategory> list);

    /**
     * 查询所有已启用的分类
     *
     * @return 已启用的分类列表
     */
    List<MaterialCategory> listEnabledCategories();

    /**
     * 根据分类编码查询分类
     *
     * @param categoryCode 分类编码
     * @return 分类实体
     */
    MaterialCategory getByCategoryCode(String categoryCode);
}
