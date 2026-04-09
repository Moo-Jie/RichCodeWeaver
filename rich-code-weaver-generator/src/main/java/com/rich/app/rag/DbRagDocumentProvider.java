package com.rich.app.rag;

import com.rich.ai.rag.RagDocumentProvider;
import com.rich.common.constant.RagConstant;
import com.rich.app.service.RagDocumentService;
import com.rich.model.entity.RagDocument;
import com.rich.model.enums.RagDocumentBizTypeEnum;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于数据库的 RAG 文档加载器实现类
 *
 * @author DuRuiChi
 * @create 2026/3/27
 * @see RagDocumentProvider 接口定义
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class DbRagDocumentProvider implements RagDocumentProvider {

    @Resource
    private RagDocumentService ragDocumentService;

    /**
     * 从数据库加载并转换所有启用的 RAG 文档
     *
     * @return RAG 文档列表
     */
    @Override
    public List<Document> loadDocuments() {
        List<RagDocument> dbDocs = ragDocumentService.listEnabledDocuments();
        if (dbDocs.isEmpty()) {
            log.warn("【DbRagDocumentProvider】数据库中未找到已启用的 RAG 文档（isEnabled=1）");
            return new ArrayList<>();
        }

        List<Document> documents = new ArrayList<>();
        for (RagDocument dbDoc : dbDocs) {
            if (dbDoc.getDocContent() == null || dbDoc.getDocContent().isBlank()) {
                log.warn("【DbRagDocumentProvider】文档 [{}] 内容为空，跳过", dbDoc.getDocTitle());
                continue;
            }

            String codeGenType = getCodeGenTypeOrDefault(dbDoc);
            String title = getDocumentTitleOrDefault(dbDoc);
            String bizType = getBizTypeOrDefault(dbDoc);

            Metadata metadata = buildMetadata(title, bizType, codeGenType);

            documents.add(Document.from(dbDoc.getDocContent(), metadata));

            log.info("【DbRagDocumentProvider】加载文档: [{}] → codeGenType={}, 内容长度: {} 字符",
                    title, codeGenType, dbDoc.getDocContent().length());
        }

        log.info("【DbRagDocumentProvider】共加载 {} 个文档", documents.size());
        return documents;
    }

    /**
     * 获取文档代码生成类型，没有时使用默认值
     *
     * @param ragDocument 文档实体
     * @return 代码生成类型
     */
    private String getCodeGenTypeOrDefault(RagDocument ragDocument) {
        String codeGenType = ragDocument.getCodeGenType();
        return (codeGenType == null || codeGenType.isBlank())
                ? RagConstant.DEFAULT_CODE_GEN_TYPE
                : codeGenType;
    }

    /**
     * 获取文档标题，没有时使用默认值
     *
     * @param ragDocument 文档实体
     * @return 文档标题
     */
    private String getDocumentTitleOrDefault(RagDocument ragDocument) {
        return ragDocument.getDocTitle() != null ? ragDocument.getDocTitle() : RagConstant.DEFAULT_DOCUMENT_TITLE;
    }

    /**
     * 获取业务类型，没有时回退到代码生成业务
     *
     * @param ragDocument 文档实体
     * @return 业务类型
     */
    private String getBizTypeOrDefault(RagDocument ragDocument) {
        String bizType = ragDocument.getBizType();
        return (bizType == null || bizType.isBlank())
                ? RagDocumentBizTypeEnum.CODE_GEN.getValue()
                : bizType;
    }

    /**
     * 构造文档元数据
     *
     * @param title 文档标题
     * @param bizType 业务类型
     * @param codeGenType 代码生成类型
     * @return 元数据
     */
    private Metadata buildMetadata(String title, String bizType, String codeGenType) {
        Metadata metadata = new Metadata();
        metadata.put(RagConstant.METADATA_BIZ_TYPE, bizType);
        metadata.put(RagConstant.METADATA_CODE_GEN_TYPE, codeGenType);
        metadata.put(RagConstant.METADATA_SOURCE, title);
        metadata.put(RagConstant.METADATA_TITLE, title);
        return metadata;
    }
}
