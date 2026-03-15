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
 * 使用百度通用图片搜索API
 * 开发文档：https://cloud.baidu.com/doc/IMAGESEARCH/s/Lk3bczuw0
 *
 * @author DuRuiChi
 * @create 2025/9/11
 **/
@Slf4j
@Component
public class ImageSearchTool extends BaseTool {

    // 注入百度图片搜索API密钥
    @Value("${baidu.image-search-api.api-key:}")
    private String baiduApiKey;

    // 注入百度图片搜索API名称
    @Value("${baidu.image-search-api.api-name:}")
    private String baiduApiName;

    // 注入百度图片搜索API地址
    @Value("${baidu.image-search-api.base-url:}")
    private String baiduBaseUrl;

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
        if (baiduApiKey == null || baiduApiKey.isEmpty()) {
            log.warn("百度图片搜索API密钥未配置，无法搜索图片");
            return imageList;
        }
        // 设置每次搜索返回的图片数量
        int searchCount = 12;

        // 调用百度图片搜索API
        try {
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.set("keyword", query);
            requestBody.set("pn", 0);  // 分页页号，从0开始
            requestBody.set("rn", searchCount);  // 返回结果数量

            // 发送POST请求
            HttpResponse response = HttpRequest.post(baiduBaseUrl)
                    .header("Content-Type", "application/json")
                    .header("Authorization", baiduApiKey)
                    .body(requestBody.toString())
                    .execute();

            // 检查HTTP响应是否成功（状态码200）
            if (response.isOk()) {
                // 解析JSON响应
                JSONObject result = JSONUtil.parseObj(response.body());
                
                // 检查是否有错误
                if (result.containsKey("error_code")) {
                    log.error("百度图片搜索API返回错误: {}", result.getStr("error_msg"));
                    return imageList;
                }
                
                // 获取图片数组
                JSONArray images = result.getJSONArray("data");
                if (images == null || images.isEmpty()) {
                    log.warn("未找到相关图片");
                    return imageList;
                }

                // 遍历所有图片结果
                for (int i = 0; i < images.size(); i++) {
                    JSONObject image = images.getJSONObject(i);

                    // 构建图片资源对象并添加到列表
                    imageList.add(ImageResource.builder()
                            .category(ImageCategoryEnum.CONTENT)           // 设置图片分类为内容图片
                            .description(image.getStr("title", query))     // 使用图片标题或搜索关键词作为描述
                            .url(image.getStr("thumbURL"))                 // 使用缩略图URL
                            .build());
                }
            } else {
                log.error("百度图片搜索API请求失败，状态码: {}", response.getStatus());
            }
        } catch (Exception e) {
            // 记录API调用失败的错误日志
            log.error("百度图片搜索API调用失败: {}", e.getMessage(), e);
        }

        // 返回搜索到的图片列表（可能为空列表）
        return imageList;
    }
}
