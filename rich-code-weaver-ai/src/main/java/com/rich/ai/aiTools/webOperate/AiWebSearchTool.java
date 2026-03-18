package com.rich.ai.aiTools.webOperate;

import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rich.ai.aiTools.BaseTool;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 供 AI 调用的联网搜索工具，通过百度千帆平台搜索API集成
 * API 文档：https://cloud.baidu.com/doc/qianfan/s/2mh4su4uy
 *
 * @author DuRuiChi
 * @create 2025/7/5
 * @update 2026/3/18
 */
@Slf4j
@Component
public class AiWebSearchTool extends BaseTool {

    /**
     * 默认的搜索结果返回数量
     */
    private static final int DEFAULT_RESULT_COUNT = 10;

    /**
     * 百度千帆平台AppBuilder API Key，从配置文件注入
     */
    @Value("${baidu.qianfan.appbuilder-api-key:}")
    private String appBuilderApiKey;

    /**
     * 百度千帆搜索API的基础URL
     */
    private static final String QIANFAN_SEARCH_URL = "https://qianfan.baidubce.com/v2/ai_search/web_search";

    @Override
    public String getToolName() {
        return "searchWeb";
    }

    @Override
    public String getToolDisplayName() {
        return "联网搜索工具";
    }

    @Override
    public String getResultMsg(JSONObject arguments) {
        String query = arguments.getStr("query");
        query = query == null || query.isEmpty() ? "" : "\n[\n" + query + "\n]\n";
        return String.format("[工具调用结束] %s %s", "成功搜索以下关键词", query);
    }

    /**
     * 执行百度千帆搜索引擎查询
     *
     * @param query 搜索关键词
     * @return 格式化后的搜索结果（包含JSON数组的字符串）
     * @throws IllegalArgumentException 如果搜索参数无效
     */
    @Tool("通过搜索引擎搜索最新的在线信息")
    public String searchWeb(@P("要查询的问题") String query) {
        // 参数校验
        if (query == null || query.trim().isEmpty()) {
            log.error("搜索查询不能为空");
            throw new IllegalArgumentException("搜索查询不能为空");
        }
        if (appBuilderApiKey == null || appBuilderApiKey.isEmpty()) {
            log.error("百度千帆AppBuilder API密钥未配置");
            return "百度千帆AppBuilder API密钥未配置，请检查配置";
        }

        try {
            // 执行HTTP POST请求
            log.info("正在通过百度千帆搜索API搜索: {}", query);

            // 构建请求体（千帆平台格式）
            JSONObject requestBody = new JSONObject();

            // 构建messages数组
            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.set("content", query);
            message.set("role", "user");
            messages.add(message);
            requestBody.set("messages", messages);

            // 设置搜索源为百度搜索v2
            requestBody.set("search_source", "baidu_search_v2");

            // 设置资源类型过滤器
            JSONArray resourceTypeFilter = new JSONArray();
            JSONObject webFilter = new JSONObject();
            webFilter.set("type", "web");
            webFilter.set("top_k", DEFAULT_RESULT_COUNT);
            resourceTypeFilter.add(webFilter);
            requestBody.set("resource_type_filter", resourceTypeFilter);

            // 发送POST请求（使用X-Appbuilder-Authorization认证头）
            HttpResponse response = HttpRequest.post(QIANFAN_SEARCH_URL)
                    .header("X-Appbuilder-Authorization", "Bearer " + appBuilderApiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody.toString())
                    .timeout(30000)  // 30秒超时
                    .execute();

            // 检查HTTP状态码
            if (!response.isOk()) {
                log.error("百度千帆搜索API请求失败，状态码: {}, 响应: {}", response.getStatus(), response.body());
                return "搜索服务返回错误，状态码: " + response.getStatus() + ", 响应: " + response.body();
            }

            // 解析并处理搜索结果
            return processSearchResponse(response.body());
        } catch (HttpException e) {
            log.error("百度千帆搜索网络异常: {}", e.getMessage(), e);
            return "搜索服务暂时不可用，请稍后再试。错误详情: " + e.getMessage();
        } catch (JSONException e) {
            log.error("解析搜索结果失败: {}", e.getMessage(), e);
            return "搜索结果解析失败。错误详情: " + e.getMessage();
        } catch (Exception e) {
            log.error("百度千帆搜索未知异常: {}", e.getMessage(), e);
            return "搜索过程中发生未知错误: " + e.getMessage();
        }
    }

    /**
     * 处理千帆平台API响应并提取有用信息
     *
     * @param response API返回的JSON字符串
     * @return 格式化后的搜索结果
     */
    private String processSearchResponse(String response) {
        JSONObject jsonObject = JSONUtil.parseObj(response);

        // 检查API返回的错误
        if (jsonObject.containsKey("error_code")) {
            String error = jsonObject.getStr("error_msg", "未知错误");
            log.error("百度千帆搜索API返回错误: {}", error);
            return "搜索服务返回错误: " + error;
        }

        // 获取references数组（千帆平台的搜索结果格式）
        JSONArray references = jsonObject.getJSONArray("references");
        if (references == null || references.isEmpty()) {
            log.warn("未找到相关搜索结果");
            return "未找到相关搜索结果";
        }

        // 确定实际结果数量
        int resultCount = Math.min(DEFAULT_RESULT_COUNT, references.size());
        List<JSONObject> resultList = new ArrayList<>();

        // 提取并格式化关键信息
        for (int i = 0; i < resultCount; i++) {
            JSONObject item = references.getJSONObject(i);
            // 只处理web类型的结果
            if ("web".equals(item.getStr("type"))) {
                JSONObject formatted = new JSONObject();
                formatted.set("id", item.getInt("id", i + 1));
                formatted.set("title", item.getStr("title", "无标题"));
                formatted.set("link", item.getStr("url", ""));
                formatted.set("snippet", item.getStr("content", "无摘要"));
                formatted.set("date", item.getStr("date", ""));
                resultList.add(formatted);
            }
        }

        if (resultList.isEmpty()) {
            log.warn("未找到web类型的搜索结果");
            return "未找到相关搜索结果";
        }

        // 返回格式化的JSON数组
        return JSONUtil.toJsonStr(resultList);
    }
}