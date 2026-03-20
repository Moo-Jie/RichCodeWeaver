package com.rich.template.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.model.dto.prompttemplate.PromptTemplateQueryRequest;
import com.rich.model.entity.PromptTemplate;
import com.rich.model.vo.PromptTemplateVO;
import com.rich.template.mapper.PromptTemplateMapper;
import com.rich.template.service.PromptTemplateService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 提示词模板 服务实现
 */
@Service
public class PromptTemplateServiceImpl extends ServiceImpl<PromptTemplateMapper, PromptTemplate>
        implements PromptTemplateService {

    @Override
    public PromptTemplateVO getPromptTemplateVO(PromptTemplate promptTemplate) {
        if (promptTemplate == null) {
            return null;
        }
        PromptTemplateVO vo = new PromptTemplateVO();
        BeanUtil.copyProperties(promptTemplate, vo);
        return vo;
    }

    @Override
    public List<PromptTemplateVO> getPromptTemplateVOList(List<PromptTemplate> list) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        return list.stream().map(this::getPromptTemplateVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(PromptTemplateQueryRequest queryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create().from(PromptTemplate.class);
        if (queryRequest == null) {
            return queryWrapper;
        }
        if (queryRequest.getId() != null) {
            queryWrapper.where("id = ?", queryRequest.getId());
        }
        if (StrUtil.isNotBlank(queryRequest.getTemplateName())) {
            queryWrapper.where("templateName LIKE ?", "%" + queryRequest.getTemplateName() + "%");
        }
        if (StrUtil.isNotBlank(queryRequest.getMatchIdentity())) {
            queryWrapper.where("matchIdentity = ?", queryRequest.getMatchIdentity());
        }
        if (StrUtil.isNotBlank(queryRequest.getMatchIndustry())) {
            queryWrapper.where("matchIndustry = ?", queryRequest.getMatchIndustry());
        }
        if (queryRequest.getIsEnabled() != null) {
            queryWrapper.where("isEnabled = ?", queryRequest.getIsEnabled());
        }
        queryWrapper.orderBy("sortOrder ASC");
        return queryWrapper;
    }

    @Override
    public List<PromptTemplateVO> listMatchedTemplates(String userIdentity, String userIndustry) {
        List<PromptTemplate> result = new ArrayList<>();

        // 1. 精确匹配：行业 + 身份
        if (StrUtil.isNotBlank(userIndustry) && StrUtil.isNotBlank(userIdentity)) {
            QueryWrapper exactQuery = QueryWrapper.create()
                    .from(PromptTemplate.class)
                    .where("matchIndustry = ? AND matchIdentity = ? AND isEnabled = 1", userIndustry, userIdentity)
                    .orderBy("sortOrder ASC");
            result.addAll(list(exactQuery));
        }

        // 2. 行业匹配（不限身份）
        if (StrUtil.isNotBlank(userIndustry)) {
            QueryWrapper industryQuery = QueryWrapper.create()
                    .from(PromptTemplate.class)
                    .where("matchIndustry = ? AND matchIdentity IS NULL AND isEnabled = 1", userIndustry)
                    .orderBy("sortOrder ASC");
            result.addAll(list(industryQuery));
        }

        // 3. 通用模板（不限行业不限身份）
        QueryWrapper universalQuery = QueryWrapper.create()
                .from(PromptTemplate.class)
                .where("matchIndustry IS NULL AND matchIdentity IS NULL AND isEnabled = 1")
                .orderBy("sortOrder ASC");
        result.addAll(list(universalQuery));

        // 去重（按id）
        List<PromptTemplate> deduped = result.stream()
                .collect(Collectors.toMap(PromptTemplate::getId, t -> t, (a, b) -> a))
                .values().stream()
                .sorted((a, b) -> {
                    // 精确匹配优先级最高
                    int scoreA = getMatchScore(a, userIdentity, userIndustry);
                    int scoreB = getMatchScore(b, userIdentity, userIndustry);
                    if (scoreA != scoreB) return scoreB - scoreA;
                    return (a.getSortOrder() != null ? a.getSortOrder() : 0) - (b.getSortOrder() != null ? b.getSortOrder() : 0);
                })
                .collect(Collectors.toList());

        return getPromptTemplateVOList(deduped);
    }

    private int getMatchScore(PromptTemplate t, String identity, String industry) {
        int score = 0;
        if (t.getMatchIndustry() != null && t.getMatchIndustry().equals(industry)) score += 2;
        if (t.getMatchIdentity() != null && t.getMatchIdentity().equals(identity)) score += 1;
        return score;
    }
}
