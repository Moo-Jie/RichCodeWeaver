package com.rich.app.controller;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.rich.ai.rag.RagDocumentIndexCreationService;
import com.rich.common.constant.RagConstant;
import com.rich.app.service.RagDocumentService;
import com.rich.common.constant.UserConstant;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.common.model.BaseResponse;
import com.rich.common.model.DeleteRequest;
import com.rich.common.utils.ResultUtils;
import com.rich.model.annotation.AuthCheck;
import com.rich.model.dto.ragdocument.RagDocumentAddRequest;
import com.rich.model.dto.ragdocument.RagDocumentQueryRequest;
import com.rich.model.dto.ragdocument.RagDocumentUpdateRequest;
import com.rich.model.entity.RagDocument;
import com.rich.model.enums.RagDocumentBizTypeEnum;
import com.rich.model.vo.RagDocumentVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * RAG 知识库文档管理接口
 * 提供知识库文档的增删改查，以及手动触发向量重新索引的功能
 *
 * @author DuRuiChi
 * @create 2026/3/27
 */
@Slf4j
@RestController
@RequestMapping("/generator/rag")
public class RagDocumentController {

    /**
     * 默认启用状态
     */
    private static final int DEFAULT_ENABLED_VALUE = 1;

    /**
     * 默认排序值
     */
    private static final int DEFAULT_SORT_ORDER = 0;

    @Resource
    private RagDocumentService ragDocumentService;

    /**
     * RAG 索引创建服务（可选注入，rag.enabled=false 时为 null）
     */
    @Autowired(required = false)
    private RagDocumentIndexCreationService ragDocumentIndexCreationService;

    /**
     * 新增知识库文档（管理员）
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addRagDocument(@RequestBody RagDocumentAddRequest addRequest,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(addRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(addRequest.getDocTitle() == null || addRequest.getDocTitle().isBlank(),
                ErrorCode.PARAMS_ERROR, "文档标题不能为空");
        RagDocument ragDocument = buildRagDocument(addRequest);
        applyAddDefaults(ragDocument);
        boolean result = ragDocumentService.save(ragDocument);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(ragDocument.getId());
    }

    /**
     * 更新知识库文档（管理员）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateRagDocument(@RequestBody RagDocumentUpdateRequest updateRequest) {
        if (updateRequest == null || updateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        RagDocument ragDocument = buildRagDocument(updateRequest);
        retainExistingBizTypeIfBlank(ragDocument, updateRequest.getId());
        boolean result = ragDocumentService.updateById(ragDocument);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 删除知识库文档（管理员）
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteRagDocument(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = ragDocumentService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取文档详情（管理员）
     */
    @GetMapping("/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<RagDocumentVO> getRagDocumentVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        RagDocument ragDocument = getRagDocumentByIdOrThrow(id);
        return ResultUtils.success(ragDocumentService.getRagDocumentVO(ragDocument));
    }

    /**
     * 分页查询知识库文档（管理员）
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<RagDocumentVO>> listRagDocumentByPage(
            @RequestBody RagDocumentQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = queryRequest.getPageNum();
        long pageSize = queryRequest.getPageSize();
        Page<RagDocument> page = ragDocumentService.page(
                Page.of(pageNum, pageSize),
                ragDocumentService.getQueryWrapper(queryRequest));
        Page<RagDocumentVO> voPage = buildRagDocumentVOPage(page, pageNum, pageSize);
        return ResultUtils.success(voPage);
    }

    /**
     * 手动触发向量库重新索引（管理员）
     * 清空现有向量数据，重新从数据库读取所有启用文档并写入 PGVector
     * 适用于：文档内容更新、新增/删除文档后需要同步向量库
     */
    @PostMapping("/reindex")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> reindexAll() {
        ThrowUtils.throwIf(ragDocumentIndexCreationService == null,
                ErrorCode.OPERATION_ERROR, RagConstant.RAG_DISABLED_MESSAGE);
        try {
            log.info("【RAG 重新索引】管理员手动触发向量库重新索引...");
            ragDocumentIndexCreationService.reindexAll();
            return ResultUtils.success(RagConstant.RAG_REINDEX_SUCCESS_MESSAGE);
        } catch (Exception e) {
            log.error("【RAG 重新索引】重新索引失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, buildReindexFailedMessage(e));
        }
    }

    /**
     * 按 ID 查询知识库文档，不存在时抛异常
     *
     * @param id 文档 ID
     * @return 知识库文档
     */
    private RagDocument getRagDocumentByIdOrThrow(long id) {
        RagDocument ragDocument = ragDocumentService.getById(id);
        ThrowUtils.throwIf(ragDocument == null, ErrorCode.NOT_FOUND_ERROR);
        return ragDocument;
    }

    /**
     * 组装重新索引失败提示
     *
     * @param e 异常对象
     * @return 失败提示
     */
    private String buildReindexFailedMessage(Exception e) {
        return "重新索引失败：" + e.getMessage();
    }

    /**
     * 构建文档实体
     *
     * @param source 请求对象
     * @return 文档实体
     */
    private RagDocument buildRagDocument(Object source) {
        RagDocument ragDocument = new RagDocument();
        BeanUtil.copyProperties(source, ragDocument);
        return ragDocument;
    }

    /**
     * 为新增文档补默认值
     *
     * @param ragDocument 文档实体
     */
    private void applyAddDefaults(RagDocument ragDocument) {
        if (ragDocument.getBizType() == null || ragDocument.getBizType().isBlank()) {
            ragDocument.setBizType(RagDocumentBizTypeEnum.CODE_GEN.getValue());
        }
        if (ragDocument.getDocContent() == null) {
            ragDocument.setDocContent("");
        }
        if (ragDocument.getIsEnabled() == null) {
            ragDocument.setIsEnabled(DEFAULT_ENABLED_VALUE);
        }
        if (ragDocument.getSortOrder() == null) {
            ragDocument.setSortOrder(DEFAULT_SORT_ORDER);
        }
    }

    /**
     * 更新时如果业务类型为空，则沿用旧值
     *
     * @param ragDocument 待更新文档
     * @param documentId 文档 ID
     */
    private void retainExistingBizTypeIfBlank(RagDocument ragDocument, Long documentId) {
        if (ragDocument.getBizType() == null || ragDocument.getBizType().isBlank()) {
            RagDocument oldDoc = ragDocumentService.getById(documentId);
            if (oldDoc != null && oldDoc.getBizType() != null) {
                ragDocument.setBizType(oldDoc.getBizType());
            }
        }
    }

    /**
     * 构造文档 VO 分页结果
     *
     * @param page 实体分页
     * @param pageNum 页号
     * @param pageSize 页大小
     * @return VO 分页结果
     */
    private Page<RagDocumentVO> buildRagDocumentVOPage(Page<RagDocument> page, long pageNum, long pageSize) {
        return new Page<>(
                page.getRecords().stream()
                        .map(ragDocumentService::getRagDocumentVO)
                        .toList(),
                pageNum, pageSize, page.getTotalRow());
    }
}
