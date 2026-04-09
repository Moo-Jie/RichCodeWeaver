package com.rich.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.app.mapper.RagDocumentMapper;
import com.rich.app.service.RagDocumentService;
import com.rich.model.dto.ragdocument.RagDocumentQueryRequest;
import com.rich.model.entity.RagDocument;
import com.rich.model.enums.RagDocumentBizTypeEnum;
import com.rich.model.vo.RagDocumentVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 知识库文档 Service 实现
 *
 * @author DuRuiChi
 * @create 2026/3/27
 */
@Service
public class RagDocumentServiceImpl extends ServiceImpl<RagDocumentMapper, RagDocument>
        implements RagDocumentService {

    @Override
    public QueryWrapper getQueryWrapper(RagDocumentQueryRequest queryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create().from(RagDocument.class);
        if (queryRequest == null) {
            return queryWrapper;
        }
        if (StrUtil.isNotBlank(queryRequest.getBizType())) {
            queryWrapper.where("bizType = ?", queryRequest.getBizType());
        }
        if (StrUtil.isNotBlank(queryRequest.getDocTitle())) {
            queryWrapper.where("docTitle LIKE ?", "%" + queryRequest.getDocTitle() + "%");
        }
        if (StrUtil.isNotBlank(queryRequest.getCodeGenType())) {
            queryWrapper.where("codeGenType = ?", queryRequest.getCodeGenType());
        }
        if (queryRequest.getIsEnabled() != null) {
            queryWrapper.where("isEnabled = ?", queryRequest.getIsEnabled());
        }
        queryWrapper.orderBy("sortOrder ASC");
        return queryWrapper;
    }

    @Override
    public RagDocumentVO getRagDocumentVO(RagDocument ragDocument) {
        if (ragDocument == null) {
            return null;
        }
        if (StrUtil.isBlank(ragDocument.getBizType())) {
            ragDocument.setBizType(RagDocumentBizTypeEnum.CODE_GEN.getValue());
        }
        RagDocumentVO vo = new RagDocumentVO();
        BeanUtil.copyProperties(ragDocument, vo);
        return vo;
    }

    @Override
    public List<RagDocumentVO> getRagDocumentVOList(List<RagDocument> list) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        return list.stream().map(this::getRagDocumentVO).collect(Collectors.toList());
    }

    @Override
    public List<RagDocument> listEnabledDocuments() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .from(RagDocument.class)
                .where("isEnabled = 1")
                .orderBy("sortOrder ASC");
        return list(queryWrapper);
    }
}
