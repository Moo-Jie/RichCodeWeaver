package com.rich.template.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.rich.model.dto.prompttemplate.PromptTemplateQueryRequest;
import com.rich.model.entity.PromptTemplate;
import com.rich.model.vo.PromptTemplateVO;

import java.util.List;

/**
 * 提示词模板 服务层
 * 提供提示词模板的业务逻辑处理，包括模板匹配、查询等功能
 *
 * @author DuRuiChi
 * @since 2026-03-12
 */
public interface PromptTemplateService extends IService<PromptTemplate> {

    /**
     * 获取提示词模板 VO
     *
     * @param promptTemplate 提示词模板实体
     * @return 提示词模板VO
     */
    PromptTemplateVO getPromptTemplateVO(PromptTemplate promptTemplate);

    /**
     * 获取提示词模板 VO 列表
     *
     * @param list 提示词模板实体列表
     * @return 提示词模板VO列表
     */
    List<PromptTemplateVO> getPromptTemplateVOList(List<PromptTemplate> list);

    /**
     * 构造查询条件
     *
     * @param queryRequest 查询请求参数
     * @return 查询条件包装器
     */
    QueryWrapper getQueryWrapper(PromptTemplateQueryRequest queryRequest);

    /**
     * 根据用户身份和行业匹配模板列表
     * 优先匹配 行业+身份，次者匹配 行业（不限身份），最后匹配通用模板
     *
     * @param userIdentity 用户身份
     * @param userIndustry 用户行业
     * @return 匹配的模板列表
     */
    List<PromptTemplateVO> listMatchedTemplates(String userIdentity, String userIndustry);
}
