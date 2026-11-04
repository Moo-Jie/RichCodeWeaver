package com.rich.app.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.rich.model.dto.ragdocument.RagDocumentQueryRequest;
import com.rich.model.entity.RagDocument;
import com.rich.model.vo.RagDocumentVO;

import java.util.List;

/**
 * RAG 知识库文档 Service
 *
 * @author DuRuiChi
 * @create 2026/3/27
 */
public interface RagDocumentService extends IService<RagDocument> {

    /**
     * 构建查询条件
     *
     * @param queryRequest 查询请求
     * @return QueryWrapper
     */
    QueryWrapper getQueryWrapper(RagDocumentQueryRequest queryRequest);

    /**
     * 将实体转换为 VO
     *
     * @param ragDocument 实体
     * @return VO
     */
    RagDocumentVO getRagDocumentVO(RagDocument ragDocument);

    /**
     * 批量转换为 VO 列表
     *
     * @param list 实体列表
     * @return VO 列表
     */
    List<RagDocumentVO> getRagDocumentVOList(List<RagDocument> list);

    /**
     * 查询所有已启用的文档（用于 RAG 摄入）
     *
     * @return 已启用的文档列表
     */
    List<RagDocument> listEnabledDocuments();
}
