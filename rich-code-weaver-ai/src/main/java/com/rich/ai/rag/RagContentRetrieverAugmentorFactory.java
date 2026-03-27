package com.rich.ai.rag;

import com.rich.ai.config.RagConfig;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * RAG 知识库 内容检索增强器工厂
 * 负责 ：文档过滤和检索 + 查询增强和关联
 *
 * @author DuRuiChi
 * @create 2026/3/26
 * @see RagConfig RAG 核心配置（提供 EmbeddingModel 和 EmbeddingStore）
 * @see RagDocumentIndexCreationService 文档摄入服务（索引阶段）
 **/
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class RagContentRetrieverAugmentorFactory {

    /**
     * 检索结果的最大返回数量
     * 返回 Top-5 最相关的文档片段注入到提示词中
     * 过多会增加 token 消耗和噪声，过少可能遗漏关键信息
     **/
    private static final int MAX_RESULTS = 5;

    /**
     * 检索结果的最低相似度分数阈值（余弦相似度，范围 0~1）
     * 低于此阈值的结果会被过滤掉，避免注入与用户查询关联度低的内容
     * 0.6 是相对保守的阈值，能有效过滤无关内容同时保留有用信息
     **/
    private static final double MIN_SCORE = 0.6;

    /**
     * RAG 内容注入的提示词模板
     * 将检索到的知识库内容以权威参考信息的形式附加到用户消息之后
     **/
    private static final String RAG_INJECTION_PROMPT_TEMPLATE =
            "{{userMessage}}\n\n" +
                    "---\n" +
                    "【权威参考知识】以下内容来自系统知识库，是你生成代码时必须严格遵循的开发规范与约束，不可违反或忽略：\n\n" +
                    "{{contents}}\n" +
                    "---";

    /**
     * Embedding 向量模型，检索阶段用于将用户查询向量化
     **/
    @Resource(name = "ragEmbeddingModel")
    private EmbeddingModel embeddingModel;

    /**
     * PGVector 向量存储，检索阶段从中搜索相似文档片段
     **/
    @Resource(name = "ragEmbeddingStore")
    private EmbeddingStore<TextSegment> embeddingStore;

    /**
     * 根据代码生成类型创建 RetrievalAugmentor（检索增强器）
     * 这是 RAG 管道的核心组件，集成到 AiServices 后会自动在每次 AI 调用前执行：
     * 1. 将用户查询向量化
     * 2. 在 PGVector 中搜索相似的知识库片段（按 codeGenType 过滤）
     * 3. 将检索到的片段注入到用户消息中
     * 4. 将增强后的消息发送给 AI 模型
     *
     * @return RetrievalAugmentor 检索增强器实例，用于注入 AiServices.builder()
     * @author DuRuiChi
     * @create 2026/3/26
     **/
    public RetrievalAugmentor createRetrievalAugmentor(String codeGenTypeName) {
        log.info("【RAG 检索增强】为代码生成类型 {} 创建 RetrievalAugmentor", codeGenTypeName);

        // 1.构建 ContentRetriever（内容检索器）
        // 基于 PGVector 的向量相似度检索，附加 codeGenType 元数据过滤
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)       // 向量存储（PGVector）
                .embeddingModel(embeddingModel)       // 向量模型（用于查询向量化）
                .maxResults(MAX_RESULTS)              // 最多返回 5 个最相关的片段
                .minScore(MIN_SCORE)                  // 最低相似度阈值，过滤不相关内容
                // 元数据过滤器：只检索与当前代码生成类型匹配的知识库文档片段
                // 或者匹配通用类型（GENERAL）的文档，确保通用规范也能被检索到
                .filter(metadataKey("codeGenType").isEqualTo(codeGenTypeName)
                        .or(metadataKey("codeGenType").isEqualTo("GENERAL")))
                .build();

        // 2.构建 ContentInjector（内容注入器）
        // 使用自定义提示词模板，将检索到的知识库内容以权威参考形式注入用户消息
        ContentInjector contentInjector = DefaultContentInjector.builder()
                // 自定义注入模板，明确标注为权威参考知识
                .promptTemplate(PromptTemplate.from(RAG_INJECTION_PROMPT_TEMPLATE))
                // 在注入内容中包含来源文件名元数据，方便追溯和调试
                .metadataKeysToInclude(List.of("source", "title"))
                .build();

        // 3.组装 RetrievalAugmentor（检索增强器）
        // DefaultRetrievalAugmentor 是 langchain4j 的标准实现，封装了完整的检索增强管道：
        //   用户查询 → ContentRetriever 检索 → ContentInjector 注入 → 增强后的消息
        RetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)  // 内容检索器
                .contentInjector(contentInjector)    // 内容注入器
                .build();

        log.info("【RAG 检索增强】RetrievalAugmentor 创建完成，codeGenType={}, maxResults={}, minScore={}",
                codeGenTypeName, MAX_RESULTS, MIN_SCORE);

        return augmentor;
    }
}
