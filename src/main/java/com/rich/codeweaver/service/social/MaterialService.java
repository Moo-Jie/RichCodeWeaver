package com.rich.codeweaver.service.social;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.rich.codeweaver.model.dto.material.MaterialQueryRequest;
import com.rich.codeweaver.model.entity.Material;
import com.rich.codeweaver.model.vo.MaterialVO;

import java.util.List;

/**
 * 素材 Service
 *
 * @author DuRuiChi
 * @create 2026/3/30
 */
public interface MaterialService extends IService<Material> {

    /**
     * 构建查询条件
     *
     * @param queryRequest 查询请求
     * @return QueryWrapper
     */
    QueryWrapper getQueryWrapper(MaterialQueryRequest queryRequest);

    /**
     * 将实体转换为 VO
     *
     * @param material 实体
     * @return VO
     */
    MaterialVO getMaterialVO(Material material);

    /**
     * 批量转换为 VO 列表
     *
     * @param list 实体列表
     * @return VO 列表
     */
    List<MaterialVO> getMaterialVOList(List<Material> list);

    /**
     * 查询用户的素材列表
     *
     * @param userId 用户id
     * @return 素材列表
     */
    List<Material> listByUserId(Long userId);

    /**
     * 根据id列表查询素材
     *
     * @param ids 素材id列表
     * @return 素材列表
     */
    List<Material> listByIds(List<Long> ids);

    /**
     * 增加素材使用次数
     *
     * @param materialId 素材id
     */
    void incrementUseCount(Long materialId);

    /**
     * 批量增加素材使用次数
     *
     * @param materialIds 素材id列表
     */
    void batchIncrementUseCount(List<Long> materialIds);

    /**
     * 将素材列表格式化为提示词文本
     * 用于拼接到对话提示词中
     *
     * @param materials 素材列表
     * @return 格式化后的提示词文本
     */
    String formatMaterialsForPrompt(List<Material> materials);
}
