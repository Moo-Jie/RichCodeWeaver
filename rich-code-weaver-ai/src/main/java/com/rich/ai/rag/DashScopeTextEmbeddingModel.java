package com.rich.ai.rag;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.embeddings.TextEmbeddingResultItem;
import com.rich.ai.config.RagConfig;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 阿里云 DashScope 文本向量模型适配器
 * 将 DashScope 原生 SDK 的 {@link TextEmbedding} 包装为 langchain4j 的 {@link EmbeddingModel} 接口，
 * 使得 text-embedding-v4 等不支持 OpenAI 兼容模式的文本向量模型可以无缝集成到 langchain4j 的 RAG 管道中。
 *
 * text-embedding-v4 是通义实验室基于 Qwen3 训练的多语言文本统一向量模型，
 * 相较 V3 版本在文本检索、聚类、分类性能大幅提升（MTEB 评测提升 15%~40%），
 * 支持 64~2048 维用户自定义向量维度。
 *
 * 核心优势：使用 DashScope 原生 {@link TextEmbedding#call(TextEmbeddingParam)} 接口，
 * 通过 {@link TextEmbeddingParam#builder()}.texts() 方法原生支持批量文本向量化，
 * 单次请求最多处理 {@value #MAX_BATCH_SIZE} 条文本，大幅减少网络往返次数。
 *
 * @author DuRuiChi
 * @create 2026/3/27
 * @see RagConfig RAG 核心配置（创建本适配器实例）
 * @see TextEmbedding DashScope 原生文本向量 API
 **/
@Slf4j
public class DashScopeTextEmbeddingModel implements EmbeddingModel {

    /**
     * DashScope TextEmbedding API 单次请求的最大文本数量限制
     * 超过此数量时自动分批处理，每批独立调用 API
     **/
    private static final int MAX_BATCH_SIZE = 10;

    /**
     * DashScope 文本向量 API 客户端
     * 线程安全，可复用
     **/
    private final TextEmbedding textEmbedding;

    /**
     * 向量模型名称（如 text-embedding-v4）
     **/
    private final String modelName;

    /**
     * DashScope API 密钥
     **/
    private final String apiKey;

    /**
     * 请求的向量维度（text-embedding-v4 支持 64~2048 自定义维度）
     * <=0 时使用模型默认维度
     **/
    private final int requestDimension;

    /**
     * @param modelName        向量模型名称（如 text-embedding-v4）
     * @param apiKey           DashScope API 密钥
     * @param requestDimension 请求的向量维度（text-embedding-v4 支持 64~2048），<=0 时使用模型默认维度
     **/
    public DashScopeTextEmbeddingModel(String modelName, String apiKey, int requestDimension) {
        this.modelName = modelName;
        this.apiKey = apiKey;
        this.requestDimension = requestDimension;
        this.textEmbedding = new TextEmbedding();
    }

    /**
     * 批量将文本片段转换为向量表示
     * 利用 DashScope TextEmbedding API 的原生批量能力（texts() 方法），
     * 单次请求最多处理 {@value #MAX_BATCH_SIZE} 条文本，超出时自动分批
     *
     * @param textSegments 待向量化的文本片段列表
     * @return 包含所有向量的响应对象，向量顺序与输入一一对应
     * @author DuRuiChi
     * @create 2026/3/27
     **/
    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        if (textSegments == null || textSegments.isEmpty()) {
            return Response.from(Collections.emptyList());
        }

        log.info("【RAG Embedding】开始批量向量化，共 {} 条文本，分批大小: {}",
                textSegments.size(), MAX_BATCH_SIZE);

        // 提取所有文本内容
        List<String> allTexts = textSegments.stream()
                .map(TextSegment::text)
                .collect(Collectors.toList());

        // 分批调用 DashScope API（每批最多 MAX_BATCH_SIZE 条）
        List<Embedding> allEmbeddings = new ArrayList<>(allTexts.size());
        int totalBatches = (allTexts.size() + MAX_BATCH_SIZE - 1) / MAX_BATCH_SIZE;

        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            int fromIndex = batchIndex * MAX_BATCH_SIZE;
            int toIndex = Math.min(fromIndex + MAX_BATCH_SIZE, allTexts.size());
            List<String> batchTexts = allTexts.subList(fromIndex, toIndex);

            log.info("【RAG Embedding】处理第 {}/{} 批，本批 {} 条文本（总进度 {}/{}）",
                    batchIndex + 1, totalBatches, batchTexts.size(), fromIndex, allTexts.size());

            try {
                List<Embedding> batchEmbeddings = callBatchEmbedding(batchTexts);
                allEmbeddings.addAll(batchEmbeddings);
            } catch (Exception e) {
                log.error("【RAG Embedding】第 {}/{} 批向量化失败: {}", batchIndex + 1, totalBatches, e.getMessage(), e);
                throw new RuntimeException("DashScope TextEmbedding 批量调用失败: " + e.getMessage(), e);
            }
        }

        log.info("【RAG Embedding】批量向量化完成，共生成 {} 个向量", allEmbeddings.size());
        return Response.from(allEmbeddings);
    }

    /**
     * 返回向量维度
     * 优先使用配置中指定的维度值
     *
     * @return 向量维度
     **/
    @Override
    public int dimension() {
        if (requestDimension > 0) {
            return requestDimension;
        }
        // text-embedding-v4 默认维度为 1024
        return 1024;
    }

    /**
     * 调用 DashScope TextEmbedding API 批量处理一组文本
     *
     * @param texts 待向量化的文本列表（最多 {@value #MAX_BATCH_SIZE} 条）
     * @return 按输入顺序排列的 Embedding 列表
     * @throws Exception API 调用异常
     **/
    private List<Embedding> callBatchEmbedding(List<String> texts) throws Exception {
        // 构建批量请求参数
        TextEmbeddingParam.TextEmbeddingParamBuilder<?, ?> paramBuilder = TextEmbeddingParam.builder()
                .model(modelName)           // 模型名称（text-embedding-v4）
                .apiKey(apiKey)             // DashScope API 密钥
                .texts(texts);              // 批量文本输入

        // 如果配置了自定义维度，传入 API（text-embedding-v4 支持 64~2048）
        if (requestDimension > 0) {
            paramBuilder.dimension(requestDimension);
        }

        TextEmbeddingParam param = paramBuilder.build();

        // 调用 DashScope 原生批量 API
        TextEmbeddingResult result = textEmbedding.call(param);

        // 提取结果：result.output.embeddings → List<TextEmbeddingResultItem>
        List<TextEmbeddingResultItem> resultItems = result.getOutput().getEmbeddings();
        if (resultItems == null || resultItems.isEmpty()) {
            throw new RuntimeException("DashScope 返回空的 embedding 结果，requestId: " + result.getRequestId());
        }

        // 按 textIndex 排序，确保输出顺序与输入一一对应
        resultItems.sort((a, b) -> {
            int idxA = a.getTextIndex() != null ? a.getTextIndex() : 0;
            int idxB = b.getTextIndex() != null ? b.getTextIndex() : 0;
            return Integer.compare(idxA, idxB);
        });

        // 转换为 langchain4j Embedding 对象（List<Double> → float[]）
        List<Embedding> embeddings = new ArrayList<>(resultItems.size());
        for (TextEmbeddingResultItem item : resultItems) {
            List<Double> vector = item.getEmbedding();
            if (vector == null || vector.isEmpty()) {
                throw new RuntimeException("DashScope 返回的第 " + item.getTextIndex() + " 条 embedding 为空");
            }

            float[] floatVector = new float[vector.size()];
            for (int i = 0; i < vector.size(); i++) {
                floatVector[i] = vector.get(i).floatValue();
            }
            embeddings.add(Embedding.from(floatVector));
        }

        return embeddings;
    }
}
