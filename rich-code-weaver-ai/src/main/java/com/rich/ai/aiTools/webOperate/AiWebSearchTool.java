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
 * 供 AI 调用的联网搜索工具，通过百度搜索API集成
 * API 文档：https://cloud.baidu.com/doc/WENXINWORKSHOP/s/Nlks5zkzu
 *
 * @author DuRuiChi
 * @create 2025/7/5
 */
@Slf4j
@Component
public class AiWebSearchTool extends BaseTool {

    /**
     * 默认的搜索结果返回数量
     */
    private static final int DEFAULT_RESULT_COUNT = 5;

    /**
     * 百度搜索API的认证密钥，从配置文件注入
     */
    @Value("${baidu.search-api.api-key:}")
    private String apiKey;

    /**
     * 百度搜索API的名称，从配置文件注入
     */
    @Value("${baidu.search-api.api-name:}")
    private String apiName;

    /**
     * 百度搜索API的基础URL，从配置文件注入
     */
    @Value("${baidu.search-api.base-url:}")
    private String baseUrl;

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
     * 执行百度搜索引擎查询
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
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("百度搜索API密钥未配置");
            return "百度搜索API密钥未配置，请检查配置";
        }

        try {
            // 执行HTTP POST请求
            log.info("正在通过百度搜索API搜索: {}", query);
            
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.set("query", query);
            requestBody.set("top_n", DEFAULT_RESULT_COUNT);
            
            // 发送请求
            HttpResponse response = HttpRequest.post(baseUrl)
                    .header("Content-Type", "application/json")
                    .header("Authorization", apiKey)
                    .body(requestBody.toString())
                    .execute();

            // 解析并处理搜索结果
            return processSearchResponse(response.body());
        } catch (HttpException e) {
            log.error("百度搜索网络异常: {}", e.getMessage());
            return "搜索服务暂时不可用，请稍后再试。错误详情: " + e.getMessage();
        } catch (JSONException e) {
            log.error("解析搜索结果失败: {}", e.getMessage());
            return "搜索结果解析失败。错误详情: " + e.getMessage();
        } catch (Exception e) {
            log.error("百度搜索未知异常: {}", e.getMessage());
            return "搜索过程中发生未知错误: " + e.getMessage();
        }
    }

    /**
     * 处理API响应并提取有用信息
     *
     * @param response API返回的JSON字符串
     * @return 格式化后的搜索结果
     */
    private String processSearchResponse(String response) {
        JSONObject jsonObject = JSONUtil.parseObj(response);

        // 检查API返回的错误
        if (jsonObject.containsKey("error_code")) {
            String error = jsonObject.getStr("error_msg");
            log.error("百度搜索API返回错误: {}", error);
            return "搜索服务返回错误: " + error;
        }

        // 获取搜索结果列表
        JSONArray results = jsonObject.getJSONArray("results");
        if (results == null || results.isEmpty()) {
            log.warn("未找到相关搜索结果");
            return "未找到相关搜索结果";
        }

        // 确定实际结果数量（不超过默认值）
        int resultCount = Math.min(DEFAULT_RESULT_COUNT, results.size());
        List<JSONObject> resultList = new ArrayList<>();

        // 提取并格式化关键信息
        for (int i = 0; i < resultCount; i++) {
            JSONObject item = results.getJSONObject(i);
            JSONObject formatted = new JSONObject();
            formatted.set("id", i + 1);
            formatted.set("title", item.getStr("title"));
            formatted.set("link", item.getStr("url"));
            formatted.set("snippet", item.getStr("content", "无摘要"));
            resultList.add(formatted);
        }

        // 返回格式化的JSON数组
        return JSONUtil.toJsonStr(resultList);
    }
}