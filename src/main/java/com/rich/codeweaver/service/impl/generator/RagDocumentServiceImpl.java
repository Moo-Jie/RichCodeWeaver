package com.rich.codeweaver.service.impl.generator;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.codeweaver.mapper.RagDocumentMapper;
import com.rich.codeweaver.model.dto.ragdocument.RagDocumentQueryRequest;
import com.rich.codeweaver.model.entity.RagDocument;
import com.rich.codeweaver.model.enums.RagDocumentBizTypeEnum;
import com.rich.codeweaver.model.vo.RagDocumentVO;
import com.rich.codeweaver.service.generator.RagDocumentService;
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

    /**
     * 构建文档查询条件
     *
     * @param queryRequest 查询请求
     * @return 查询条件
     */
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

    /**
     * 将文档实体转换为 VO
     *
     * @param ragDocument 文档实体
     * @return 文档 VO
     */
    @Override
    public RagDocumentVO getRagDocumentVO(RagDocument ragDocument) {
        if (ragDocument == null) {
            return null;
        }
        fillDefaultBizType(ragDocument);
        RagDocumentVO vo = new RagDocumentVO();
        BeanUtil.copyProperties(ragDocument, vo);
        return vo;
    }

    /**
     * 批量将文档实体转换为 VO 列表
     *
     * @param list 文档实体列表
     * @return 文档 VO 列表
     */
    @Override
    public List<RagDocumentVO> getRagDocumentVOList(List<RagDocument> list) {
        if (isEmptyList(list)) {
            return new ArrayList<>();
        }
        return list.stream().map(this::getRagDocumentVO).collect(Collectors.toList());
    }

    /**
     * 查询已启用的 RAG 文档
     *
     * @return 已启用文档列表
     */
    @Override
    public List<RagDocument> listEnabledDocuments() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .from(RagDocument.class)
                .where("isEnabled = 1")
                .orderBy("sortOrder ASC");
        return list(queryWrapper);
    }

    /**
     * 补全文档默认业务类型
     *
     * @param ragDocument 文档实体
     */
    private void fillDefaultBizType(RagDocument ragDocument) {
        if (StrUtil.isBlank(ragDocument.getBizType())) {
            ragDocument.setBizType(RagDocumentBizTypeEnum.CODE_GEN.getValue());
        }
    }

    /**
     * 判断文档列表是否为空
     *
     * @param list 文档列表
     * @return 是否为空
     */
    private boolean isEmptyList(List<RagDocument> list) {
        return list == null || list.isEmpty();
    }
}
