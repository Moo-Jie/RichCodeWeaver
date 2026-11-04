package com.rich.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.app.mapper.MaterialCategoryMapper;
import com.rich.app.mapper.MaterialMapper;
import com.rich.app.service.MaterialCategoryService;
import com.rich.model.dto.materialcategory.MaterialCategoryQueryRequest;
import com.rich.model.entity.Material;
import com.rich.model.entity.MaterialCategory;
import com.rich.model.vo.MaterialCategoryVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 素材分类 Service 实现
 *
 * @author DuRuiChi
 * @create 2026/3/30
 */
@Service
@Slf4j
public class MaterialCategoryServiceImpl extends ServiceImpl<MaterialCategoryMapper, MaterialCategory>
        implements MaterialCategoryService {

    @Resource
    private MaterialMapper materialMapper;

    /**
     * 构建查询条件
     */
    @Override
    public QueryWrapper getQueryWrapper(MaterialCategoryQueryRequest queryRequest) {
        if (queryRequest == null) {
            return QueryWrapper.create();
        }

        Long id = queryRequest.getId();
        String categoryName = queryRequest.getCategoryName();
        String categoryCode = queryRequest.getCategoryCode();
        Integer isEnabled = queryRequest.getIsEnabled();

        return QueryWrapper.create()
                .eq("id", id)
                .eq("categoryCode", categoryCode)
                .eq("isEnabled", isEnabled)
                .like("categoryName", categoryName)
                .orderBy("sortOrder", true);
    }

    /**
     * 将实体转换为 VO
     */
    @Override
    public MaterialCategoryVO getMaterialCategoryVO(MaterialCategory materialCategory) {
        if (materialCategory == null) {
            return null;
        }
        MaterialCategoryVO vo = new MaterialCategoryVO();
        BeanUtil.copyProperties(materialCategory, vo);
        return vo;
    }

    /**
     * 批量转换为 VO 列表（包含素材数量统计）
     */
    @Override
    public List<MaterialCategoryVO> getMaterialCategoryVOList(List<MaterialCategory> list) {
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }

        // 查询每个分类下的素材数量
        List<Long> categoryIds = list.stream()
                .map(MaterialCategory::getId)
                .collect(Collectors.toList());

        // 统计每个分类的素材数量
        Map<Long, Long> countMap = materialMapper.selectListByQuery(
                QueryWrapper.create().in("categoryId", categoryIds)
        ).stream().collect(Collectors.groupingBy(Material::getCategoryId, Collectors.counting()));

        return list.stream().map(category -> {
            MaterialCategoryVO vo = getMaterialCategoryVO(category);
            // 设置素材数量
            vo.setMaterialCount(countMap.getOrDefault(category.getId(), 0L).intValue());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 查询所有已启用的分类
     */
    @Override
    public List<MaterialCategory> listEnabledCategories() {
        return this.list(QueryWrapper.create()
                .eq("isEnabled", 1)
                .orderBy("sortOrder", true));
    }

    /**
     * 根据分类编码查询分类
     */
    @Override
    public MaterialCategory getByCategoryCode(String categoryCode) {
        if (StrUtil.isBlank(categoryCode)) {
            return null;
        }
        return this.getOne(QueryWrapper.create().eq("categoryCode", categoryCode));
    }
}
