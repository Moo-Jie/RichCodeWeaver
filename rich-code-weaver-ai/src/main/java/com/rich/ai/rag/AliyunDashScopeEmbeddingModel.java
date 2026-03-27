package com.rich.ai.rag;

import com.alibaba.dashscope.embeddings.MultiModalEmbedding;
import com.alibaba.dashscope.embeddings.MultiModalEmbeddingItemText;
import com.alibaba.dashscope.embeddings.MultiModalEmbeddingParam;
import com.alibaba.dashscope.embeddings.MultiModalEmbeddingResult;
import com.rich.ai.config.RagConfig;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 阿里云 DashScope 向量模型
 * 将 DashScope 原生 SDK 的 MultiModalEmbedding 包装为 langchain4j 的 EmbeddingModel 接口，
 * 使得 qwen3-vl-embedding 等不支持 OpenAI 兼容模式的向量模型可以无缝集成到 langchain4j 的 RAG 管道中。
 *
 * @author DuRuiChi
 * @create 2026/3/27
 * @see RagConfig RAG 核心配置（创建本适配器实例）
 * @see MultiModalEmbedding DashScope 原生多模态向量 API
 **/
@Slf4j
public class AliyunDashScopeEmbeddingModel implements EmbeddingModel {

    /**
     * DashScope 多模态向量 API 客户端
     * 线程安全，可复用
     **/
    private final MultiModalEmbedding multiModalEmbedding;

    /**
     * 向量模型名称（如 qwen3-vl-embedding）
     **/
    private final String modelName;

    /**
     * DashScope API 密钥
     **/
    private final String apiKey;

    /**
     * 向量维度（用于 {@link #dimension()} 方法快速返回，避免额外 API 调用）
     * 若未指定（<=0），则首次调用时通过实际 embedding 结果推断
     **/
    private final int configuredDimension;

    /**
     * 缓存的向量维度值（首次 embedding 后自动填充）
     **/
    private volatile int cachedDimension = -1;

    /**
     * @param modelName           向量模型名称（如 qwen3-vl-embedding）
     * @param apiKey              DashScope API 密钥
     * @param configuredDimension 预期的向量维度，<=0 时自动推断
     **/
    public AliyunDashScopeEmbeddingModel(String modelName, String apiKey, int configuredDimension) {
        this.modelName = modelName;
        this.apiKey = apiKey;
        this.configuredDimension = configuredDimension;
        this.multiModalEmbedding = new MultiModalEmbedding();
    }

    /**
     * 批量将文本片段转换为向量表示
     * 逐条调用 DashScope MultiModalEmbedding API（该 API 不支持批量输入）
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

        List<Embedding> embeddings = new ArrayList<>(textSegments.size());

        for (int i = 0; i < textSegments.size(); i++) {
            TextSegment segment = textSegments.get(i);
            try {
                Embedding embedding = embedSingleText(segment.text());
                embeddings.add(embedding);

                // 首次成功后缓存实际维度
                if (cachedDimension < 0) {
                    cachedDimension = embedding.dimension();
                    log.info("【RAG Embedding】检测到向量维度: {}", cachedDimension);
                }

                // 每 10 条输出一次进度日志（避免大量文档时日志刷屏）
                if ((i + 1) % 10 == 0) {
                    log.info("【RAG Embedding】已完成 {}/{} 条文本向量化", i + 1, textSegments.size());
                }
            } catch (Exception e) {
                log.error("【RAG Embedding】第 {} 条文本向量化失败（共 {} 条），文本前50字符: {}",
                        i + 1, textSegments.size(),
                        segment.text().substring(0, Math.min(50, segment.text().length())),
                        e);
                throw new RuntimeException("DashScope MultiModal Embedding 调用失败: " + e.getMessage(), e);
            }
        }

        log.info("【RAG Embedding】批量向量化完成，共 {} 条", embeddings.size());
        return Response.from(embeddings);
    }

    /**
     * 返回向量维度
     * 优先使用配置值，否则使用首次 embedding 的实际维度
     *
     * @return 向量维度
     **/
    @Override
    public int dimension() {
        // 优先使用配置中指定的维度
        if (configuredDimension > 0) {
            return configuredDimension;
        }
        // 如果已缓存实际维度，直接返回
        if (cachedDimension > 0) {
            return cachedDimension;
        }
        // 未知维度时，通过一次实际调用来获取
        try {
            Embedding testEmbedding = embedSingleText("dimension test");
            cachedDimension = testEmbedding.dimension();
            return cachedDimension;
        } catch (Exception e) {
            log.warn("【RAG Embedding】获取向量维度失败，返回默认值 1536: {}", e.getMessage());
            return 1536;
        }
    }

    /**
     * 调用 DashScope MultiModalEmbedding API 将单条文本转换为向量
     *
     * @param text 待向量化的文本
     * @return langchain4j Embedding 对象
     * @throws Exception API 调用异常
     **/
    private Embedding embedSingleText(String text) throws Exception {
        // 构建多模态文本输入项
        MultiModalEmbeddingItemText textContent = new MultiModalEmbeddingItemText(text);

        // 构建 API 请求参数
        MultiModalEmbeddingParam param = MultiModalEmbeddingParam.builder()
                .model(modelName)                                       // 模型名称（qwen3-vl-embedding）
                .apiKey(apiKey)                                         // DashScope API 密钥
                .contents(Collections.singletonList(textContent))       // 文本输入内容
                .build();

        // 调用 DashScope 原生 API
        MultiModalEmbeddingResult result = multiModalEmbedding.call(param);

        // 提取向量结果：result.output.embedding → List<Double>
        List<Double> vector = result.getOutput().getEmbedding();
        if (vector == null || vector.isEmpty()) {
            throw new RuntimeException("DashScope 返回空的 embedding 向量，requestId: " + result.getRequestId());
        }

        // 转换为 float 数组（langchain4j Embedding 使用 float[]）
        float[] floatVector = new float[vector.size()];
        for (int j = 0; j < vector.size(); j++) {
            floatVector[j] = vector.get(j).floatValue();
        }

        return Embedding.from(floatVector);
    }
}
