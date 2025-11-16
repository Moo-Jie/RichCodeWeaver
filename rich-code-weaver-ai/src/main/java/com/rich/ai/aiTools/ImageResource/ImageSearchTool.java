package com.rich.ai.aiTools.ImageResource;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rich.ai.aiTools.BaseTool;
import com.rich.model.entity.ImageResource;
import com.rich.model.enums.ImageCategoryEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片搜索工具
 * 支持 AI 通过工具调用的方式搜索图片
 * 开发文档：https://www.pexels.com/api/
 *
 * @author DuRuiChi
 * @create 2025/9/11
 **/
@Slf4j
@Component
public class ImageSearchTool extends BaseTool {

    // Pexels API 的基础URL
    private static final String PEXELS_API_URL = "https://api.pexels.com/v1/search";

    // 注入 Pexels API 密钥
    @Value("${pexels.api-key:}")
    private String pexelsApiKey;

    @Override
    public String getToolName() {
        return "searchImages";
    }

    @Override
    public String getToolDisplayName() {
        return "图片搜索工具";
    }

    @Override
    public String getResultMsg(JSONObject arguments) {
        String query = arguments.getStr("query");
        query = query == null || query.isEmpty() ? "" : "\n[\n" + query + "\n]\n";
        return String.format("[工具调用结束] %s %s", "成功搜索以下关键词的图片", query);
    }

    /**
     * 搜索内容相关的图片，用于网站内容展示
     *
     * @param query 搜索关键词，用于指定要搜索的图片内容
     * @return 返回图片资源列表，包含图片URL、描述和分类信息
     */
    @Tool("搜索内容相关的图片，用于网站内容展示")
    public List<ImageResource> searchImages(@P("清晰、凝练的搜索关键词") String query) {
        // 初始化图片列表
        List<ImageResource> imageList = new ArrayList<>();
        if (pexelsApiKey == null || pexelsApiKey.isEmpty()) {
            log.warn("Pexels API 密钥未配置，无法搜索图片");
            return imageList;
        }
        // 设置每次搜索返回的图片数量
        int searchCount = 12;

        // 调用 Pexels API，使用 try-with-resources 确保HTTP响应资源被正确释放
        // 文档：https://www.pexels.com/api/documentation/#photos-search
        try (HttpResponse response = HttpRequest.get(PEXELS_API_URL)
                .header("Authorization", pexelsApiKey) // 设置认证头
                .form("query", query)                  // 设置搜索关键词
                .form("per_page", searchCount)        // 设置每页返回结果数量
                .form("page", 1)                      // 设置页码（第一页）
                .execute()) {

            // 检查HTTP响应是否成功（状态码200）
            if (response.isOk()) {
                // 解析JSON响应
                JSONObject result = JSONUtil.parseObj(response.body());
                // 获取图片数组
                JSONArray photos = result.getJSONArray("photos");

                // 遍历所有图片结果
                for (int i = 0; i < photos.size(); i++) {
                    JSONObject photo = photos.getJSONObject(i);
                    // 获取图片的不同尺寸URL
                    JSONObject src = photo.getJSONObject("src");

                    // 构建图片资源对象并添加到列表
                    imageList.add(ImageResource.builder()
                            .category(ImageCategoryEnum.CONTENT)           // 设置图片分类为内容图片
                            .description(photo.getStr("alt", query))       // 使用图片的alt文本或搜索关键词作为描述
                            .url(src.getStr("medium"))                     // 使用中等尺寸的图片URL
                            .build());
                }
            }
        } catch (Exception e) {
            // 记录API调用失败的错误日志
            log.error("Pexels API 调用失败: {}", e.getMessage(), e);
        }

        // 返回搜索到的图片列表（可能为空列表）
        return imageList;
    }
}
