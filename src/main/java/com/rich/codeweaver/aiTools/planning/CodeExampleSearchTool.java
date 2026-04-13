package com.rich.codeweaver.aiTools.planning;

import cn.hutool.json.JSONObject;
import com.rich.codeweaver.aiTools.BaseTool;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 代码示例搜索工具
 * 从 RAG 知识库中搜索相关的代码示例和开发规范
 * 
 * 使用场景：
 * - 搜索特定功能的实现示例（如轮播图、表单验证）
 * - 查找最佳实践和开发规范
 * - 获取特定技术栈的代码模板
 *
 * @author DuRuiChi
 * @create 2026/3/28
 **/
@Slf4j
@Component
public class CodeExampleSearchTool extends BaseTool {

    @Autowired(required = false)
    @Resource(name = "ragEmbeddingModel")
    private EmbeddingModel embeddingModel;

    @Autowired(required = false)
    @Resource(name = "ragEmbeddingStore")
    private EmbeddingStore<TextSegment> embeddingStore;

    @Override
    public String getToolName() {
        return "searchCodeExample";
    }

    @Override
    public String getToolDisplayName() {
        return "代码示例搜索";
    }

    @Override
    public String getResultMsg(JSONObject arguments, String result) {
        String query = arguments.getStr("query");
        return "[工具调用结束] 搜索代码示例: " + (query != null ? query : "");
    }

    /**
     * 搜索代码示例
     * 
     * @param query 搜索查询，描述需要的功能或代码示例
     * @param maxResults 最大返回结果数（1-10）
     * @return 搜索到的代码示例
     */
    @Tool("从知识库搜索代码示例和开发规范。在实现特定功能前先搜索相关示例，避免重复造轮子。")
    public String searchCodeExample(
            @P("搜索查询，描述需要的功能或代码示例，如：'Vue3轮播图组件'、'表单验证'、'响应式布局'")
            String query,
            @P("最大返回结果数，1-10之间，默认3")
            Integer maxResults) {

        // 检查 RAG 是否启用
        if (embeddingModel == null || embeddingStore == null) {
            return "提示：RAG 知识库未启用，无法搜索代码示例。请直接根据经验编写代码。";
        }

        if (query == null || query.trim().isEmpty()) {
            return "错误：搜索查询不能为空";
        }

        // 限制结果数量
        int limit = maxResults != null ? Math.min(Math.max(maxResults, 1), 10) : 3;

        try {
            log.info("[CodeExampleSearch] 搜索代码示例，query={}, maxResults={}", query, limit);

            // 将查询向量化
            Embedding queryEmbedding = embeddingModel.embed(query).content();

            // 在向量存储中搜索
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(limit)
                    .minScore(0.5)  // 最低相似度阈值
                    .build();

            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
            List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

            if (matches.isEmpty()) {
                return "未找到与 '" + query + "' 相关的代码示例。请根据经验编写代码。";
            }

            // 格式化搜索结果
            StringBuilder result = new StringBuilder();
            result.append("─".repeat(50)).append("\n");
            result.append("📚 代码示例搜索结果（共 ").append(matches.size()).append(" 条）\n");
            result.append("─".repeat(50)).append("\n\n");

            for (int i = 0; i < matches.size(); i++) {
                EmbeddingMatch<TextSegment> match = matches.get(i);
                TextSegment segment = match.embedded();
                
                String title = segment.metadata().getString("title");
                String source = segment.metadata().getString("source");
                double score = match.score();

                result.append("【示例 ").append(i + 1).append("】");
                if (title != null) {
                    result.append(" ").append(title);
                }
                result.append(" (相似度: ").append(String.format("%.2f", score)).append(")\n");
                
                if (source != null) {
                    result.append("来源: ").append(source).append("\n");
                }
                
                result.append("─".repeat(30)).append("\n");
                result.append(segment.text()).append("\n\n");
            }

            result.append("─".repeat(50)).append("\n");
            result.append("请参考以上示例编写代码，注意遵循其中的最佳实践。");

            log.info("[CodeExampleSearch] 搜索完成，返回 {} 条结果", matches.size());
            return result.toString();

        } catch (Exception e) {
            log.error("[CodeExampleSearch] 搜索失败", e);
            return "搜索代码示例时发生错误: " + e.getMessage() + "。请根据经验编写代码。";
        }
    }
}
