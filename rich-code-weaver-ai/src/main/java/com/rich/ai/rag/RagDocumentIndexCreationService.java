package com.rich.ai.rag;

import com.rich.ai.config.RagConfig;
import com.rich.common.constant.RagConstant;
import com.rich.model.enums.RagDocumentBizTypeEnum;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RAG 知识库 索引创建服务
 * 负责 ：文档收集与切割（ETL） + 向量转换与存储
 *
 * @author DuRuiChi
 * @create 2026/3/26
 * @see RagConfig RAG 核心配置
 * @see RagContentRetrieverAugmentorFactory 检索增强工厂（检索阶段）
 **/
@Slf4j
@Service
@ConditionalOnProperty(prefix = RagAiConstant.RAG_CONFIG_PREFIX,
        name = RagAiConstant.RAG_ENABLED_NAME,
        havingValue = RagAiConstant.RAG_ENABLED_VALUE)
public class RagDocumentIndexCreationService {

    /**
     * 文档摄入日志标识
     */
    private static final String INGEST_LOG_TAG = "RAG 文档摄入";

    /**
     * 重建索引日志标识
     */
    private static final String REINDEX_LOG_TAG = "RAG 重新索引";

    /**
     * Markdown 文件匹配表达式
     */
    private static final String MARKDOWN_GLOB_PATTERN = "glob:*.md";

    /**
     * 文件名元数据键
     */
    private static final String FILE_NAME_METADATA_KEY = "file_name";

    /**
     * 未知文件名默认值
     */
    private static final String UNKNOWN_FILE_NAME = "unknown";

    /**
     * Markdown 一级标题前缀
     */
    private static final String TITLE_PREFIX = "# ";

    /**
     * 标题提取时最多检查的行数
     */
    private static final int TITLE_SCAN_LIMIT = 5;

    /**
     * 检测向量存储是否已有数据时使用的测试文本
     */
    private static final String EMBEDDING_STORE_TEST_QUERY = "网页开发代码规范";

    /**
     * 统计片段失败时的兜底数量
     */
    private static final int FALLBACK_SEGMENT_COUNT = 1;

    /**
     * 代码生成业务类型标识
     */
    private static final String CODE_GEN_BIZ_TYPE = RagDocumentBizTypeEnum.CODE_GEN.getValue();

    /**
     * 文件名前缀 → 代码生成类型的映射表
     * 用于自动识别知识库文档对应的代码生成类型
     * key: 文件名中的关键字（匹配规则：文件名包含该关键字）
     * value: 对应的代码生成类型标识（与 CodeGeneratorTypeEnum 枚举值一致）
     **/
    private static final Map<String, String> FILENAME_TO_CODE_GEN_TYPE = Map.of(
            "html", "HTML",                    // 单 HTML 文件模式
            "multi-file", "MULTI_FILE",        // 多文件模式（HTML + CSS + JS）
            "vue-project", "VUE_PROJECT"       // Vue 项目工程模式
    );

    /**
     * Embedding 向量模型，将文本转换为高维向量
     * 在索引阶段用于将文档片段向量化
     **/
    @Resource(name = RagAiConstant.RAG_EMBEDDING_MODEL_BEAN)
    private EmbeddingModel embeddingModel;

    /**
     * PGVector 向量存储，持久化保存文本向量和原始内容
     **/
    @Resource(name = RagAiConstant.RAG_EMBEDDING_STORE_BEAN)
    private EmbeddingStore<TextSegment> embeddingStore;

    /**
     * RAG 配置属性
     **/
    @Resource
    private RagProperties ragProperties;

    /**
     * 文档来源提供者
     **/
    @Autowired
    private RagDocumentProvider ragDocumentProvider;

    /**
     * RAG 参数提供者（动态从数据库加载参数，可选注入）
     **/
    @Autowired(required = false)
    private RagParamProvider ragParamProvider;

    /**
     * 应用启动完成后自动执行文档摄入检查
     * 使用 {@link ApplicationReadyEvent} 而非 {@code @PostConstruct}，
     * 确保所有 Bean 和外部资源（数据库连接等）都已就绪
     *
     * @author DuRuiChi
     * @create 2026/3/26
     **/
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // 检查是否开启了启动时自动摄入
        if (!ragProperties.isAutoIngestOnStartup()) {
            log.info("【{}】自动摄入已关闭（rag.auto-ingest-on-startup=false），跳过", INGEST_LOG_TAG);
            return;
        }

        log.info("【{}】应用启动完成，开始检查知识库状态...", INGEST_LOG_TAG);
        try {
            // 检查向量存储中是否已有数据，避免重复摄入
            if (isEmbeddingStorePopulated()) {
                log.info("【{}】知识库已有数据，跳过自动摄入。如需重新索引，请调用 reindexAll() 方法", INGEST_LOG_TAG);
                return;
            }

            // 知识库为空，执行首次文档摄入
            log.info("【{}】知识库为空，开始首次文档摄入...", INGEST_LOG_TAG);
            ingestDocuments();
        } catch (Exception e) {
            // 摄入失败不影响主应用启动，仅记录错误日志
            // AI 服务在没有 RAG 的情况下仍可正常工作，只是不会检索知识库
            log.error("【{}】启动时自动摄入失败，AI 服务仍可正常使用但不会检索知识库: {}", INGEST_LOG_TAG,
                    e.getMessage(), e);
        }
    }

    /**
     * 执行完整的文档摄入流程
     * 读取配置路径下的所有 Markdown 文档，经过切分、元数据标注、向量化后存入 PGVector
     *
     * @author DuRuiChi
     * @create 2026/3/26
     **/
    public void ingestDocuments() {
        List<Document> enrichedDocuments = loadDocumentsForIngestion();
        if (enrichedDocuments == null) {
            return;
        }

        // 构建文档切分器
        // 使用递归分割器，按照 Markdown 文档结构进行智能切分
        // 切分优先级：段落（\n\n）→ 行（\n）→ 空格 → 字符
        // 这样能最大程度保持文档的语义完整性
        DocumentSplitter splitter = buildDocumentSplitter();

        // 使用 EmbeddingStoreIngestor 执行摄入管道
        // EmbeddingStoreIngestor 是 langchain4j 提供的标准摄入工具，封装了：
        //   文档切分（DocumentSplitter）→ 向量化（EmbeddingModel）→ 存储（EmbeddingStore）
        EmbeddingStoreIngestor ingestor = buildEmbeddingStoreIngestor(splitter);

        // 执行摄入：遍历所有文档，切分为片段，向量化后批量存入 PGVector
        ingestor.ingest(enrichedDocuments);

        // 统计摄入结果
        int totalSegments = countTotalSegments(enrichedDocuments, splitter);
        log.info("【{}】摄入完成！共处理 {} 个文档，预计生成约 {} 个文本片段，已全部存入 PGVector",
                INGEST_LOG_TAG, enrichedDocuments.size(), totalSegments);
    }

    /**
     * 重新索引所有文档（先清空再重新摄入）
     * 适用于知识库文档内容更新后需要同步更新向量存储的场景
     *
     * @author DuRuiChi
     * @create 2026/3/26
     **/
    public void reindexAll() {
        log.info("【{}】开始清空已有向量数据并重新摄入...", REINDEX_LOG_TAG);

        // 清空向量存储中的所有数据
        embeddingStore.removeAll();
        log.info("【{}】已清空向量存储", REINDEX_LOG_TAG);

        // 重新执行文档摄入
        ingestDocuments();
        log.info("【{}】重新索引完成", REINDEX_LOG_TAG);
    }

    /**
     * 检查向量存储中是否已有数据
     * 通过执行一次测试性的向量搜索来判断，搜索返回结果则表示已有数据
     *
     * 使用嵌入一段通用文本（"代码规范"）作为测试查询，
     * 如果能检索到至少一条结果，说明向量存储已被填充。
     *
     * @return true 表示已有数据，false 表示为空
     * @author DuRuiChi
     * @create 2026/3/26
     **/
    private boolean isEmbeddingStorePopulated() {
        try {
            // 使用一段与知识库内容相关的通用文本作为测试查询
            var testEmbedding = embeddingModel.embed(EMBEDDING_STORE_TEST_QUERY).content();

            // 在向量存储中搜索，只取 1 条结果，不设最低分数阈值
            var searchResult = embeddingStore.search(
                    dev.langchain4j.store.embedding.EmbeddingSearchRequest.builder()
                            .queryEmbedding(testEmbedding)
                            .maxResults(1)
                            .build()
            );

            // 如果搜索返回了结果，说明存储中已有数据
            return !searchResult.matches().isEmpty();
        } catch (Exception e) {
            // 搜索异常（如表不存在）视为无数据，后续会自动创建表并摄入
            log.debug("【{}】检测向量存储状态时异常（可能是首次启动，表尚未创建）: {}",
                    INGEST_LOG_TAG, e.getMessage());
            return false;
        }
    }

    /**
     * 从文件系统加载文档（兼容旧配置的回退方案）
     * 读取 rag.docs-path 目录下所有 .md 文件并附加元数据
     *
     * @return 带元数据的文档列表，若路径未配置或无文件则返回 null
     **/
    private List<Document> loadFromFileSystem() {
        String docsPath = ragProperties.getDocsPath();
        if (docsPath == null || docsPath.isBlank()) {
            log.warn("【{}】未配置数据库文档来源，也未配置文件路径（rag.docs-path），跳过摄入", INGEST_LOG_TAG);
            return null;
        }
        Path docsDir = Path.of(docsPath);
        if (!Files.exists(docsDir) || !Files.isDirectory(docsDir)) {
            log.warn("【{}】文档目录不存在或不是目录: {}，跳过摄入", INGEST_LOG_TAG, docsPath);
            return null;
        }
        log.info("【{}】从文件系统加载文档: {}", INGEST_LOG_TAG, docsPath);
        PathMatcher mdMatcher = FileSystems.getDefault().getPathMatcher(MARKDOWN_GLOB_PATTERN);
        List<Document> documents = FileSystemDocumentLoader.loadDocuments(
                docsDir, mdMatcher, new TextDocumentParser());
        if (documents.isEmpty()) {
            log.warn("【{}】目录 {} 下未找到 .md 文件，跳过摄入", INGEST_LOG_TAG, docsPath);
            return null;
        }
        log.info("【{}】共加载 {} 个文档，开始处理...", INGEST_LOG_TAG, documents.size());
        return enrichFileSystemDocuments(documents);
    }

    /**
     * 加载待摄入文档
     * 优先使用数据库文档来源，没有时回退到文件系统
     *
     * @return 待摄入文档列表
     */
    private List<Document> loadDocumentsForIngestion() {
        if (ragDocumentProvider != null) {
            log.info("【{}】使用数据库文档来源（RagDocumentProvider）加载文档...", INGEST_LOG_TAG);
            List<Document> enrichedDocuments = ragDocumentProvider.loadDocuments();
            if (enrichedDocuments.isEmpty()) {
                log.warn("【{}】数据库中无已启用的 RAG 文档（isEnabled=1），跳过摄入", INGEST_LOG_TAG);
                return null;
            }
            log.info("【{}】从数据库加载到 {} 个文档，开始处理...", INGEST_LOG_TAG, enrichedDocuments.size());
            return enrichedDocuments;
        }

        // 回退：从文件系统读取
        return loadFromFileSystem();
    }

    /**
     * 构建文档切分器
     *
     * @return 文档切分器
     */
    private DocumentSplitter buildDocumentSplitter() {
        int maxSegmentSize = ragParamProvider != null ? ragParamProvider.getMaxSegmentSize() : RagConstant.DEFAULT_MAX_SEGMENT_SIZE;
        int maxOverlapSize = ragParamProvider != null ? ragParamProvider.getMaxOverlapSize() : RagConstant.DEFAULT_MAX_OVERLAP_SIZE;
//        log.info("【{}】切分参数：maxSegmentSize={}, maxOverlapSize={}", INGEST_LOG_TAG, maxSegmentSize, maxOverlapSize);
        return DocumentSplitters.recursive(maxSegmentSize, maxOverlapSize);
    }

    /**
     * 构建 EmbeddingStoreIngestor
     *
     * @param splitter 文档切分器
     * @return 摄入器
     */
    private EmbeddingStoreIngestor buildEmbeddingStoreIngestor(DocumentSplitter splitter) {
        return EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter)
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
    }

    /**
     * 为文件系统文档补充元数据
     *
     * @param documents 原始文档列表
     * @return 补充元数据后的文档列表
     */
    private List<Document> enrichFileSystemDocuments(List<Document> documents) {
        List<Document> enriched = new ArrayList<>();
        for (Document doc : documents) {
            String fileName = getFileNameOrDefault(doc);
            String codeGenType = resolveCodeGenType(fileName);
            String docTitle = extractDocumentTitle(doc.text());
            Metadata enrichedMetadata = buildFileDocumentMetadata(doc, fileName, codeGenType, docTitle);
            enriched.add(Document.from(doc.text(), enrichedMetadata));
            log.info("【{}】处理文档: {} → codeGenType={}, 标题: {}, 文本长度: {} 字符",
                    INGEST_LOG_TAG, fileName, codeGenType, docTitle, doc.text().length());
        }
        return enriched;
    }

    /**
     * 根据文件名推断代码生成类型
     * 文件名中包含的关键字与 FILENAME_TO_CODE_GEN_TYPE 映射表匹配
     *
     * @param fileName 文件名（如 rag-web-dev-guide-html.md）
     * @return 代码生成类型标识（如 HTML、MULTI_FILE、VUE_PROJECT），未匹配时返回 GENERAL
     **/
    private String resolveCodeGenType(String fileName) {
        // 转为小写后进行匹配，忽略大小写差异
        String lowerName = fileName.toLowerCase();
        for (Map.Entry<String, String> entry : FILENAME_TO_CODE_GEN_TYPE.entrySet()) {
            if (lowerName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        // 未匹配到已知类型的文件，标记为通用类型
        // 通用类型的文档在所有代码生成模式下都可被检索到
        log.warn("【{}】文件 {} 未匹配到已知的代码生成类型，标记为 GENERAL", INGEST_LOG_TAG, fileName);
        return RagConstant.DEFAULT_CODE_GEN_TYPE;
    }

    /**
     * 提取 Markdown 文档的标题（第一行 # 开头的内容）
     *
     * @param text Markdown 文档原始文本
     * @return 文档标题，未找到时返回 "未知文档"
     **/
    private String extractDocumentTitle(String text) {
        if (text == null || text.isBlank()) {
            return RagConstant.DEFAULT_DOCUMENT_TITLE;
        }
        // 查找第一行 # 开头的内容作为标题
        String[] lines = text.split("\n", TITLE_SCAN_LIMIT);
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith(TITLE_PREFIX)) {
                return trimmed.substring(TITLE_PREFIX.length()).trim();
            }
        }
        return RagConstant.DEFAULT_DOCUMENT_TITLE;
    }

    /**
     * 统计文档切分后生成的总片段数（用于日志输出）
     * 仅为预估值，实际数量由 EmbeddingStoreIngestor 内部处理
     *
     * @param documents 文档列表
     * @param splitter  文档切分器
     * @return 预估的总片段数
     **/
    private int countTotalSegments(List<Document> documents, DocumentSplitter splitter) {
        int total = 0;
        for (Document doc : documents) {
            try {
                List<TextSegment> segments = splitter.split(doc);
                total += segments.size();
            } catch (Exception e) {
                // 统计失败不影响主流程
                total += FALLBACK_SEGMENT_COUNT;
            }
        }
        return total;
    }

    /**
     * 获取文件名元数据，没有时使用默认值
     *
     * @param document 文档对象
     * @return 文件名
     */
    private String getFileNameOrDefault(Document document) {
        String fileName = document.metadata().getString(FILE_NAME_METADATA_KEY);
        return fileName == null ? UNKNOWN_FILE_NAME : fileName;
    }

    /**
     * 构建文件系统文档的补充元数据
     *
     * @param document 原始文档
     * @param fileName 文件名
     * @param codeGenType 代码生成类型
     * @param docTitle 文档标题
     * @return 补充后的元数据
     */
    private Metadata buildFileDocumentMetadata(Document document, String fileName, String codeGenType, String docTitle) {
        Metadata enrichedMetadata = document.metadata().copy();
        enrichedMetadata.put(RagConstant.METADATA_BIZ_TYPE, CODE_GEN_BIZ_TYPE);
        enrichedMetadata.put(RagConstant.METADATA_CODE_GEN_TYPE, codeGenType);
        enrichedMetadata.put(RagConstant.METADATA_SOURCE, fileName);
        enrichedMetadata.put(RagConstant.METADATA_TITLE, docTitle);
        return enrichedMetadata;
    }
}
