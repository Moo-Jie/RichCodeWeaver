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
 * 使用接口盒子（apihz.cn）提供的百度图片搜索API
 * 接口文档：https://www.apihz.cn/api/apihzimgbaidu.html
 *
 * @author DuRuiChi
 * @create 2025/9/11
 * @update 2026/3/18
 **/
@Slf4j
@Component
public class ImageSearchTool extends BaseTool {

    // 接口盒子API地址
    private static final String APIHZ_BASE_URL = "https://cn.apihz.cn/api/img/apihzimgbaidu.php";
    
    // 接口盒子ID
    @Value("${apihz.image-search.id:10014138}")
    private String apihzId;

    // 接口盒子开发者KEY
    @Value("${apihz.image-search.key:f9977bd04e49b300d467d16d22607b84}")
    private String apihzKey;
    
    // 接口类型（百度源）
    private static final String API_TYPE = "apihzimgbaidu";

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
        
        // 参数校验
        if (query == null || query.trim().isEmpty()) {
            log.warn("搜索关键词为空，无法搜索图片");
            return imageList;
        }

        // 调用接口盒子图片搜索API
        try {
            log.info("正在通过接口盒子搜索图片: {}", query);
            
            // 构建GET请求URL
            // 参数说明：id=接口ID, key=开发者KEY, words=搜索关键词, limit=返回数量, page=页码
            String requestUrl = String.format("%s?id=%s&key=%s&words=%s&limit=12&page=1",
                    APIHZ_BASE_URL,
                    apihzId,
                    apihzKey,
                    query);  // 关键词会被自动URL编码

            // 发送GET请求
            HttpResponse response = HttpRequest.get(requestUrl)
                    .timeout(15000)  // 15秒超时
                    .execute();

            // 检查HTTP响应是否成功（状态码200）
            if (!response.isOk()) {
                log.error("接口盒子图片搜索API请求失败，状态码: {}, 响应: {}", response.getStatus(), response.body());
                return imageList;
            }
            
            // 记录原始响应体
            String responseBody = response.body();
            log.debug("接口盒子API响应: {}", responseBody);
            
            // 解析JSON响应
            JSONObject result = JSONUtil.parseObj(responseBody);
            
            // 检查返回状态
            Integer code = result.getInt("code");
            if (code == null || code != 200) {
                log.error("接口盒子图片搜索API返回错误: code={}, msg={}, 完整响应: {}", 
                    code, result.getStr("msg"), responseBody);
                return imageList;
            }
            
            // 获取图片URL数组（接口盒子返回的是res数组，直接包含URL字符串）
            JSONArray imageUrls = result.getJSONArray("res");
            
            if (imageUrls == null || imageUrls.isEmpty()) {
                log.warn("未找到相关图片: {}", query);
                return imageList;
            }
            
            log.info("成功获取到 {} 个图片URL", imageUrls.size());

            // 遍历所有图片URL（最多取12张）
            int maxCount = Math.min(12, imageUrls.size());
            for (int i = 0; i < maxCount; i++) {
                String imageUrl = imageUrls.getStr(i);
                
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    // 构建图片资源对象并添加到列表
                    imageList.add(ImageResource.builder()
                            .category(ImageCategoryEnum.CONTENT)           // 设置图片分类为内容图片
                            .description(query)                            // 使用搜索关键词作为描述
                            .url(imageUrl)                                 // 图片URL
                            .build());
                }
            }
            
            log.info("成功搜索到 {} 张图片", imageList.size());
            
        } catch (Exception e) {
            // 记录API调用失败的错误日志
            log.error("接口盒子图片搜索API调用失败: {}", e.getMessage(), e);
        }

        // 返回搜索到的图片列表（可能为空列表）
        return imageList;
    }
}
