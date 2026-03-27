package com.rich.app.rag;

import com.rich.ai.rag.RagDocumentProvider;
import com.rich.app.service.RagDocumentService;
import com.rich.model.entity.RagDocument;
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

            String codeGenType = dbDoc.getCodeGenType();
            if (codeGenType == null || codeGenType.isBlank()) {
                codeGenType = "GENERAL";
            }

            String title = dbDoc.getDocTitle() != null ? dbDoc.getDocTitle() : "未知文档";

            Metadata metadata = new Metadata();
            metadata.put("codeGenType", codeGenType);
            metadata.put("source", title);
            metadata.put("title", title);

            documents.add(Document.from(dbDoc.getDocContent(), metadata));

            log.info("【DbRagDocumentProvider】加载文档: [{}] → codeGenType={}, 内容长度: {} 字符",
                    title, codeGenType, dbDoc.getDocContent().length());
        }

        log.info("【DbRagDocumentProvider】共加载 {} 个文档", documents.size());
        return documents;
    }
}
