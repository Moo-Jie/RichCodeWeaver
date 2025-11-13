package com.rich.ai.aiTools.webOperate;

import cn.hutool.http.HttpException;
import cn.hutool.http.HttpUtil;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 供 AI 调用的联网搜索工具，通过 SearchAPI 集成百度搜索引擎
 *
 * @author DuRuiChi
 * @create 2025/7/5
 */
@Slf4j
@Component
public class AiWebSearchTool extends BaseTool {

    /**
     * SearchAPI 的搜索端点URL
     * API 文档：https://www.searchapi.io/docs/google
     */
    private static final String SEARCH_URL = "https://www.searchapi.io/api/v1/search";

    /**
     * 默认的搜索结果返回数量
     */
    private static final int DEFAULT_RESULT_COUNT = 5;

    /**
     * SearchAPI 的认证密钥，从配置文件注入
     */
    @Value("${search-api.api-key:}")
    private String apiKey;

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
            log.error("SearchAPI 密钥未配置");
            return "SearchAPI 密钥未配置，请检查配置";
        }

        Map<String, Object> paramMap = createRequestParams(query);

        try {
            // 执行HTTP GET请求
            log.info("正在搜索: {}", query);
            String response = HttpUtil.get(SEARCH_URL, paramMap);

            // 解析并处理搜索结果
            return processSearchResponse(response);
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
     * 创建搜索请求参数
     *
     * @param query 搜索关键词
     * @return 参数映射表
     */
    private Map<String, Object> createRequestParams(String query) {
        // 构建请求参数
        Map<String, Object> paramMap = new HashMap<>(8);
        // 添加搜索关键词
        paramMap.put("q", query);
        // 添加API密钥
        paramMap.put("api_key", apiKey);
        // 使用百度搜索引擎
        paramMap.put("engine", "baidu");
        // 国家/地区代码（中国）
        paramMap.put("gl", "cn");
        // 界面语言（简体中文）
        paramMap.put("hl", "zh-cn");
        // 明确请求结果数量
        paramMap.put("num", DEFAULT_RESULT_COUNT);
        return paramMap;
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
        if (jsonObject.containsKey("error")) {
            String error = jsonObject.getStr("error");
            log.error("SearchAPI返回错误: {}", error);
            return "搜索服务返回错误: " + error;
        }

        // 获取搜索结果列表
        JSONArray organicResults = jsonObject.getJSONArray("organic_results");
        if (organicResults == null || organicResults.isEmpty()) {
            log.warn("未找到相关搜索结果");
            return "未找到相关搜索结果";
        }

        // 确定实际结果数量（不超过默认值）
        int resultCount = Math.min(DEFAULT_RESULT_COUNT, organicResults.size());
        List<JSONObject> resultList = new ArrayList<>();

        // 提取并格式化关键信息
        for (int i = 0; i < resultCount; i++) {
            JSONObject item = organicResults.getJSONObject(i);
            JSONObject formatted = new JSONObject();
            formatted.set("id", i + 1);
            formatted.set("title", item.getStr("title"));
            formatted.set("link", item.getStr("link"));
            formatted.set("snippet", item.getStr("snippet", "无摘要"));
            resultList.add(formatted);
        }

        // 返回格式化的JSON数组
        return JSONUtil.toJsonStr(resultList);
    }
}