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
 * @create 2025/11/5
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
        // 从参数中提取搜索关键词
        String query = arguments.getStr("query");
        
        // 格式化显示文本，若关键词为空则不显示
        String displayQuery = (query == null || query.trim().isEmpty()) 
                ? "" 
                : "\n[\n" + query + "\n]\n";
        
        return String.format("[工具调用结束] %s %s", "成功搜索以下关键词", displayQuery);
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
        // 参数校验：检查搜索关键词是否为空
        if (query == null || query.trim().isEmpty()) {
            String errorMsg = "搜索查询不能为空";
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        
        // 配置校验：检查API密钥是否已配置
        if (appBuilderApiKey == null || appBuilderApiKey.trim().isEmpty()) {
            String errorMsg = "百度千帆AppBuilder API密钥未配置，请检查配置";
            log.error(errorMsg);
            return errorMsg;
        }

        try {
            log.info("正在通过百度千帆搜索API搜索关键词: {}", query);

            // 构建请求体JSON对象（符合千帆平台API规范）
            JSONObject requestBody = new JSONObject();

            // 构建messages数组（包含用户查询消息）
            JSONArray messages = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.set("content", query);  // 设置搜索内容
            userMessage.set("role", "user");    // 设置角色为用户
            messages.add(userMessage);
            requestBody.set("messages", messages);

            // 设置搜索源为百度搜索v2（千帆平台支持的搜索引擎）
            requestBody.set("search_source", "baidu_search_v2");

            // 设置资源类型过滤器（只获取web类型的搜索结果）
            JSONArray resourceTypeFilter = new JSONArray();
            JSONObject webFilter = new JSONObject();
            webFilter.set("type", "web");                      // 资源类型：网页
            webFilter.set("top_k", DEFAULT_RESULT_COUNT);      // 返回结果数量
            resourceTypeFilter.add(webFilter);
            requestBody.set("resource_type_filter", resourceTypeFilter);

            // 发送POST请求到百度千帆搜索API
            HttpResponse response = HttpRequest.post(QIANFAN_SEARCH_URL)
                    .header("X-Appbuilder-Authorization", "Bearer " + appBuilderApiKey)  // API认证头
                    .header("Content-Type", "application/json")  // 请求内容类型
                    .body(requestBody.toString())  // 请求体
                    .timeout(30000)  // 设置30秒超时时间
                    .execute();

            // 检查HTTP响应状态码是否成功
            if (!response.isOk()) {
                String errorMsg = String.format("百度千帆搜索API请求失败，状态码: %d, 响应: %s", 
                        response.getStatus(), response.body());
                log.error(errorMsg);
                return "搜索服务返回错误，状态码: " + response.getStatus();
            }
            
            // 获取响应体内容
            String responseBody = response.body();
            
            // 检查响应体是否为空
            if (responseBody == null || responseBody.trim().isEmpty()) {
                String errorMsg = "百度千帆搜索API返回空响应";
                log.error(errorMsg);
                return "搜索服务返回空结果";
            }

            // 解析并处理搜索结果
            return processSearchResponse(responseBody);
        } catch (HttpException e) {
            // 捕获HTTP网络异常（连接超时、网络不可达等）
            String errorMsg = String.format("百度千帆搜索网络异常，关键词: %s, 错误: %s", query, e.getMessage());
            log.error(errorMsg, e);
            return "搜索服务暂时不可用，请稍后再试";
        } catch (JSONException e) {
            // 捕获JSON解析异常（响应格式不正确）
            String errorMsg = String.format("解析搜索结果失败，关键词: %s, 错误: %s", query, e.getMessage());
            log.error(errorMsg, e);
            return "搜索结果解析失败，请稍后再试";
        } catch (Exception e) {
            // 捕获所有其他未预期的异常
            String errorMsg = String.format("百度千帆搜索未知异常，关键词: %s, 错误: %s", query, e.getMessage());
            log.error(errorMsg, e);
            return "搜索过程中发生未知错误: " + e.getMessage();
        }
    }

    /**
     * 处理千帆平台API响应并提取有用信息
     * 将API返回的复杂JSON结构转换为简化的搜索结果列表
     *
     * @param responseBody API返回的JSON字符串
     * @return 格式化后的搜索结果JSON数组字符串
     */
    private String processSearchResponse(String responseBody) {
        // 参数校验：检查响应体是否为空
        if (responseBody == null || responseBody.trim().isEmpty()) {
            log.warn("处理搜索响应时发现响应体为空");
            return "未找到相关搜索结果";
        }
        
        // 解析JSON响应体
        JSONObject jsonResponse = JSONUtil.parseObj(responseBody);

        // 检查API是否返回错误码
        if (jsonResponse.containsKey("error_code")) {
            String errorMsg = jsonResponse.getStr("error_msg", "未知错误");
            log.error("百度千帆搜索API返回错误: {}", errorMsg);
            return "搜索服务返回错误: " + errorMsg;
        }

        // 获取references数组（千帆平台的搜索结果存储在此字段）
        JSONArray references = jsonResponse.getJSONArray("references");
        
        // 检查是否获取到搜索结果
        if (references == null || references.isEmpty()) {
            log.warn("未找到相关搜索结果");
            return "未找到相关搜索结果";
        }

        // 确定实际需要处理的结果数量（取配置数量和实际结果数量的较小值）
        int actualResultCount = Math.min(DEFAULT_RESULT_COUNT, references.size());
        List<JSONObject> formattedResults = new ArrayList<>();

        // 遍历搜索结果并提取关键信息
        for (int i = 0; i < actualResultCount; i++) {
            JSONObject referenceItem = references.getJSONObject(i);
            
            // 跳过null项（防御性编程）
            if (referenceItem == null) {
                log.debug("跳过null搜索结果项，索引: {}", i);
                continue;
            }
            
            // 只处理web类型的搜索结果（过滤其他类型）
            String itemType = referenceItem.getStr("type");
            if ("web".equals(itemType)) {
                // 构建格式化的搜索结果对象
                JSONObject formattedResult = new JSONObject();
                formattedResult.set("id", referenceItem.getInt("id", i + 1));           // 结果ID
                formattedResult.set("title", referenceItem.getStr("title", "无标题"));   // 标题
                formattedResult.set("link", referenceItem.getStr("url", ""));           // URL链接
                formattedResult.set("snippet", referenceItem.getStr("content", "无摘要")); // 内容摘要
                formattedResult.set("date", referenceItem.getStr("date", ""));          // 发布日期
                
                formattedResults.add(formattedResult);
            }
        }

        // 检查是否有有效的web类型结果
        if (formattedResults.isEmpty()) {
            log.warn("未找到web类型的搜索结果");
            return "未找到相关搜索结果";
        }
        
        log.info("成功处理 {} 条搜索结果", formattedResults.size());

        // 返回格式化的JSON数组字符串
        return JSONUtil.toJsonStr(formattedResults);
    }
}