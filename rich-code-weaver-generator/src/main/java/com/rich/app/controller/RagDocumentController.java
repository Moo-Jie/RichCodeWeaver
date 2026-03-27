package com.rich.app.controller;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.rich.ai.rag.RagDocumentIndexCreationService;
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
@RequestMapping("/rag")
public class RagDocumentController {

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
        ThrowUtils.throwIf(addRequest.getDocContent() == null || addRequest.getDocContent().isBlank(),
                ErrorCode.PARAMS_ERROR, "文档内容不能为空");
        RagDocument ragDocument = new RagDocument();
        BeanUtil.copyProperties(addRequest, ragDocument);
        if (ragDocument.getIsEnabled() == null) {
            ragDocument.setIsEnabled(1);
        }
        if (ragDocument.getSortOrder() == null) {
            ragDocument.setSortOrder(0);
        }
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
        RagDocument ragDocument = new RagDocument();
        BeanUtil.copyProperties(updateRequest, ragDocument);
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
        RagDocument ragDocument = ragDocumentService.getById(id);
        ThrowUtils.throwIf(ragDocument == null, ErrorCode.NOT_FOUND_ERROR);
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
        Page<RagDocumentVO> voPage = new Page<>(
                page.getRecords().stream()
                        .map(ragDocumentService::getRagDocumentVO)
                        .toList(),
                pageNum, pageSize, page.getTotalRow());
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
                ErrorCode.OPERATION_ERROR, "RAG 功能未启用（rag.enabled=false），无法执行重新索引");
        try {
            log.info("【RAG 重新索引】管理员手动触发向量库重新索引...");
            ragDocumentIndexCreationService.reindexAll();
            return ResultUtils.success("向量库重新索引完成，已同步最新知识库文档");
        } catch (Exception e) {
            log.error("【RAG 重新索引】重新索引失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "重新索引失败：" + e.getMessage());
        }
    }
}
