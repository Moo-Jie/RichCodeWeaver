package com.rich.ai.rag;

import dev.langchain4j.data.document.Document;

import java.util.List;

/**
 * RAG 知识库文档来源接口
 * 解耦文档来源（文件系统 / 数据库 / 其他），由具体实现决定从哪里加载文档
 * 文档已包含 codeGenType、source、title 元数据，可直接进入切分摄入管道
 *
 * @author DuRuiChi
 * @create 2026/3/27
 * @see RagDocumentIndexCreationService 消费此接口加载文档
 */
public interface RagDocumentProvider {

    /**
     * 加载所有可用的知识库文档
     * 返回的文档对象中 Metadata 应包含：
     * - codeGenType: 代码生成类型（HTML / MULTI_FILE / VUE_PROJECT / GENERAL）
     * - source: 来源标识（文件名或文档标题）
     * - title: 文档标题
     *
     * @return 文档列表（已附加元数据）
     */
    List<Document> loadDocuments();
}
