package com.rich.codeweaver.aiTools.ImageResource;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rich.codeweaver.aiTools.BaseTool;
import com.rich.codeweaver.model.entity.ImageResource;
import com.rich.codeweaver.model.enums.ImageCategoryEnum;
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
 * @create 2026/1/11
 * @update 2026/3/18
 **/
@Slf4j
@Component
public class ImageSearchTool extends BaseTool {

    // 接口盒子API地址
    private static final String APIHZ_BASE_URL = "https://cn.apihz.cn/api/img/apihzimgbaidu.php";
    // 接口类型（百度源）
    private static final String API_TYPE = "apihzimgbaidu";
    // 接口盒子ID
    @Value("${apihz.image-search.id:10014138}")
    private String apihzId;
    // 接口盒子开发者KEY
    @Value("${apihz.image-search.key:f9977bd04e49b300d467d16d22607b84}")
    private String apihzKey;

    @Override
    public String getToolName() {
        return "searchImages";
    }

    @Override
    public String getToolDisplayName() {
        return "图片搜索工具";
    }

    @Override
    public String getResultMsg(JSONObject arguments, String result) {
        // 从参数中提取搜索关键词
        String query = arguments.getStr("query");
        String displayQuery = (query == null || query.trim().isEmpty())
                ? ""
                : "\n[\n" + query + "\n]\n";
        return String.format("[工具调用结束] %s %s", "成功搜索以下关键词的图片", displayQuery);
    }

    /**
     * 搜索内容相关的图片，用于网站内容展示
     * 使用接口盒子提供的百度图片搜索API进行图片检索
     *
     * @param query 搜索关键词，用于指定要搜索的图片内容
     * @return 返回图片资源列表，包含图片URL、描述和分类信息（失败时返回空列表）
     */
    @Tool("【重要】搜索在线图片URL，用于网站内容展示。" +
          "当用户需求涉及图片、照片、背景图、轮播图、头像、产品图等视觉内容时必须调用此工具。" +
          "禁止使用本地图片路径，必须使用此工具获取在线图片URL。")
    public List<ImageResource> searchImages(@P("清晰、凝练的搜索关键词，如：浪漫情侣照片、求婚场景、爱心背景") String query) {
        // 初始化图片列表（用于存储搜索结果）
        List<ImageResource> imageList = new ArrayList<>();

        // 参数校验：检查搜索关键词是否为空
        if (query == null || query.trim().isEmpty()) {
            log.warn("搜索关键词为空，无法搜索图片");
            return imageList;
        }

        // 配置校验：检查API密钥是否已配置
        if (apihzId == null || apihzId.trim().isEmpty() ||
                apihzKey == null || apihzKey.trim().isEmpty()) {
            log.error("接口盒子API配置未完成，无法搜索图片");
            return imageList;
        }

        // 调用接口盒子图片搜索API
        try {
            log.info("正在通过接口盒子搜索图片，关键词: {}", query);

            // 构建GET请求URL
            // 参数说明：id=接口ID, key=开发者KEY, words=搜索关键词, limit=返回数量, page=页码
            String requestUrl = String.format("%s?id=%s&key=%s&words=%s&limit=12&page=1",
                    APIHZ_BASE_URL,
                    apihzId,
                    apihzKey,
                    query);  // HttpRequest会自动进行URL编码

            // 发送GET请求到接口盒子API
            HttpResponse response = HttpRequest.get(requestUrl)
                    .timeout(15000)  // 设置15秒超时时间
                    .execute();

            // 检查HTTP响应状态码是否为200（成功）
            if (!response.isOk()) {
                log.error("接口盒子图片搜索API请求失败，状态码: {}, 响应: {}",
                        response.getStatus(), response.body());
                return imageList;
            }

            // 获取响应体内容
            String responseBody = response.body();

            // 检查响应体是否为空
            if (responseBody == null || responseBody.trim().isEmpty()) {
                log.error("接口盒子API返回空响应");
                return imageList;
            }

            log.debug("接口盒子API响应: {}", responseBody);

            // 解析JSON响应体
            JSONObject result = JSONUtil.parseObj(responseBody);

            // 检查API返回的业务状态码
            Integer code = result.getInt("code");
            if (code == null || code != 200) {
                String errorMsg = result.getStr("msg");
                log.error("接口盒子图片搜索API返回错误: code={}, msg={}", code, errorMsg);
                return imageList;
            }

            // 获取图片URL数组（接口盒子返回的res字段是字符串数组）
            JSONArray imageUrls = result.getJSONArray("res");

            // 检查是否获取到图片URL
            if (imageUrls == null || imageUrls.isEmpty()) {
                log.warn("未找到相关图片，关键词: {}", query);
                return imageList;
            }

            log.info("成功获取到 {} 个图片URL", imageUrls.size());

            // 遍历所有图片URL并构建ImageResource对象（最多取12张）
            int maxCount = Math.min(12, imageUrls.size());
            for (int i = 0; i < maxCount; i++) {
                String imageUrl = imageUrls.getStr(i);

                // 校验图片URL是否有效
                if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                    // 构建图片资源对象并添加到列表
                    ImageResource imageResource = ImageResource.builder()
                            .category(ImageCategoryEnum.CONTENT)  // 设置图片分类为内容图片
                            .description(query)                   // 使用搜索关键词作为图片描述
                            .url(imageUrl.trim())                 // 图片URL（去除首尾空格）
                            .build();
                    imageList.add(imageResource);
                }
            }

            log.info("成功搜索到 {} 张有效图片，关键词: {}", imageList.size(), query);

        } catch (Exception e) {
            // 捕获所有异常并记录详细错误日志
            log.error("接口盒子图片搜索API调用失败，关键词: {}, 错误: {}", query, e.getMessage(), e);
        }

        // 返回搜索到的图片列表（可能为空列表）
        return imageList;
    }
}
