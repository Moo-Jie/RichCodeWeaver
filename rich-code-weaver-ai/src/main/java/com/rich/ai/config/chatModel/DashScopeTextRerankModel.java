package com.rich.ai.config.chatModel;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rich.ai.config.RagEmbeddingConfig;
import dev.langchain4j.rag.content.Content;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 阿里云 DashScope 文本重排序模型适配器
 * 调用 DashScope Text-Rerank API（Qwen3-VL-Rerank），对 RAG 检索结果进行语义重排序，
 * 提升最终注入上下文的相关性与质量。
 *
 * @author DuRuiChi
 * @create 2026/5/17
 * @see RagEmbeddingConfig RAG 核心配置（创建本适配器实例）
 **/
@Slf4j
public class DashScopeTextRerankModel {

    /**
     * DashScope Rerank API 地址
     **/
    private static final String RERANK_API_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank";

    /**
     * HTTP 请求超时时间（毫秒）
     **/
    private static final int HTTP_TIMEOUT_MS = 30_000;

    /**
     * Rerank 模型名称（如 qwen3-vl-rerank）
     **/
    private final String modelName;

    /**
     * DashScope API 密钥
     **/
    private final String apiKey;

    /**
     * 重排序后返回的最大结果数
     **/
    private final int topN;

    /**
     * @param modelName Rerank 模型名称（如 qwen3-vl-rerank）
     * @param apiKey    DashScope API 密钥
     * @param topN      重排序后返回的最大结果数
     **/
    public DashScopeTextRerankModel(String modelName, String apiKey, int topN) {
        this.modelName = modelName;
        this.apiKey = apiKey;
        this.topN = topN;
    }

    /**
     * 对检索结果进行语义重排序
     * 将用户查询与检索命中的文本片段发送到 DashScope Rerank API，
     * 根据语义相关性重新排序并返回 Top-N 结果
     *
     * @param query    用户原始查询文本
     * @param contents 向量检索命中的内容列表
     * @return 重排序后的内容列表（按相关性降序，最多 topN 条）
     * @author DuRuiChi
     * @create 2026/5/17
     **/
    public List<Content> rerank(String query, List<Content> contents) {
        if (contents == null || contents.isEmpty()) {
            return Collections.emptyList();
        }

        // 提取文本内容用于 Rerank
        List<String> documents = contents.stream()
                .map(content -> content.textSegment().text())
                .toList();

        log.info("【RAG Rerank】开始重排序，查询: {}...，待排序文档数: {}，topN: {}",
                query.length() > 50 ? query.substring(0, 50) : query, documents.size(), topN);

        try {
            // 调用 DashScope Rerank API
            List<RerankResult> rerankResults = callRerankApi(query, documents);

            // 按 Rerank 结果重新排列原始 Content
            List<Content> rerankedContents = new ArrayList<>(rerankResults.size());
            for (RerankResult result : rerankResults) {
                if (result.index() >= 0 && result.index() < contents.size()) {
                    rerankedContents.add(contents.get(result.index()));
                }
            }

            log.info("【RAG Rerank】重排序完成，返回 {} 条结果（原始 {} 条），最高相关分: {}",
                    rerankedContents.size(), contents.size(),
                    rerankResults.isEmpty() ? "N/A" : String.format("%.4f", rerankResults.get(0).relevanceScore()));

            return rerankedContents;
        } catch (Exception e) {
            log.error("【RAG Rerank】重排序失败，回退到原始检索结果: {}", e.getMessage(), e);
            // 降级：返回原始结果（不影响正常流程）
            return contents;
        }
    }

    /**
     * 调用 DashScope Text-Rerank API
     *
     * @param query     用户查询
     * @param documents 待排序的文档文本列表
     * @return 重排序结果列表（按相关性降序）
     **/
    private List<RerankResult> callRerankApi(String query, List<String> documents) {
        // 构建请求体
        JSONObject input = new JSONObject();
        input.set("query", query);
        input.set("documents", documents);

        JSONObject parameters = new JSONObject();
        parameters.set("return_documents", false);
        parameters.set("top_n", topN);

        JSONObject requestBody = new JSONObject();
        requestBody.set("model", modelName);
        requestBody.set("input", input);
        requestBody.set("parameters", parameters);

        // 发送 HTTP 请求
        HttpResponse response = HttpRequest.post(RERANK_API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody.toString())
                .timeout(HTTP_TIMEOUT_MS)
                .execute();

        if (!response.isOk()) {
            throw new RuntimeException("DashScope Rerank API 返回异常状态码: " + response.getStatus()
                    + "，响应: " + response.body());
        }

        // 解析响应
        JSONObject responseBody = JSONUtil.parseObj(response.body());
        JSONObject output = responseBody.getJSONObject("output");
        if (output == null) {
            throw new RuntimeException("DashScope Rerank API 响应缺少 output 字段，响应: " + response.body());
        }

        JSONArray results = output.getJSONArray("results");
        if (results == null || results.isEmpty()) {
            log.warn("【RAG Rerank】API 返回空结果集");
            return Collections.emptyList();
        }

        // 转换为 RerankResult 列表（已按 relevance_score 降序）
        List<RerankResult> rerankResults = new ArrayList<>(results.size());
        for (int i = 0; i < results.size(); i++) {
            JSONObject item = results.getJSONObject(i);
            int index = item.getInt("index");
            double relevanceScore = item.getDouble("relevance_score");
            rerankResults.add(new RerankResult(index, relevanceScore));
        }

        return rerankResults;
    }

    /**
     * Rerank 结果记录
     *
     * @param index          原始文档在输入列表中的索引
     * @param relevanceScore 语义相关性分数（越高越相关）
     **/
    public record RerankResult(int index, double relevanceScore) {
    }
}
