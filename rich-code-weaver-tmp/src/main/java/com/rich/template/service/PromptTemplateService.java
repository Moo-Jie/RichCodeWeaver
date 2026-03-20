package com.rich.template.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.rich.model.dto.prompttemplate.PromptTemplateQueryRequest;
import com.rich.model.entity.PromptTemplate;
import com.rich.model.vo.PromptTemplateVO;

import java.util.List;

/**
 * 提示词模板 服务层
 */
public interface PromptTemplateService extends IService<PromptTemplate> {

    /**
     * 获取提示词模板 VO
     */
    PromptTemplateVO getPromptTemplateVO(PromptTemplate promptTemplate);

    /**
     * 获取提示词模板 VO 列表
     */
    List<PromptTemplateVO> getPromptTemplateVOList(List<PromptTemplate> list);

    /**
     * 构造查询条件
     */
    QueryWrapper getQueryWrapper(PromptTemplateQueryRequest queryRequest);

    /**
     * 根据用户身份和行业匹配模板列表
     * 优先匹配 行业+身份，次者匹配 行业（不限身份），最后匹配通用模板
     */
    List<PromptTemplateVO> listMatchedTemplates(String userIdentity, String userIndustry);
}
