package com.rich.ai.aiTools.webOperate;

import cn.hutool.json.JSONObject;
import com.rich.ai.aiTools.BaseTool;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 供 AI 调用的网页抓取工具，通过SearchAPI集成百度搜索引擎
 *
 * @author DuRuiChi
 * @return
 * @create 2025/11/6
 **/
@Slf4j
@Component
public class AiWebScrapingTool extends BaseTool {

    @Override
    public String getToolName() {
        return "scrapeWebPage";
    }

    @Override
    public String getToolDisplayName() {
        return "网页抓取工具";
    }

    @Override
    public String getResultMsg(JSONObject arguments) {
        // 从参数中提取网页URL
        String scrapUrl = arguments.getStr("ScrapUrl");

        // 处理URL为空的情况
        if (scrapUrl == null || scrapUrl.trim().isEmpty()) {
            return "[工具调用结束] 成功抓取网页内容";
        }

        // 对超长URL进行截取（避免日志过长）
        String displayUrl = scrapUrl;
        if (scrapUrl.length() > 100) {
            displayUrl = scrapUrl.substring(0, 100) + "...(超长省略)";
        }

        // 格式化显示文本
        String formattedUrl = "\n[\n" + displayUrl + "\n]\n";

        return String.format("[工具调用结束] %s %s", "成功抓取以下网页内容", formattedUrl);
    }

    /**
     * 抓取指定URL的网页内容
     * 使用Jsoup库进行网页抓取，提取纯文本内容（去除HTML标签），返回前2000个字符
     *
     * @param scrapUrl 要抓取的网页URL地址
     * @return 网页纯文本内容的前2000个字符，失败时返回错误信息
     */
    @Tool("抓取网页内容")
    public String scrapeWebPage(@P("要抓取的网页的 URL") String scrapUrl) {
        // 参数校验：检查URL是否为空
        if (scrapUrl == null || scrapUrl.trim().isEmpty()) {
            String errorMsg = "错误：网页URL不能为空";
            log.warn(errorMsg);
            return errorMsg;
        }

        // 参数校验：检查URL格式是否合法（简单验证）
        String normalizedUrl = scrapUrl.trim();
        if (!normalizedUrl.startsWith("http://") && !normalizedUrl.startsWith("https://")) {
            String errorMsg = "错误：URL必须以http://或https://开头 - " + normalizedUrl;
            log.warn(errorMsg);
            return errorMsg;
        }

        try {
            log.info("开始抓取网页: {}", normalizedUrl);

            // 使用Jsoup连接并获取网页纯文本内容（去除HTML标签，减少噪音）
            String textContent = Jsoup.connect(normalizedUrl)
                    .timeout(10000)  // 设置10秒超时时间
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")  // 设置User-Agent
                    .get()
                    .text();

            // 检查抓取到的内容是否为空
            if (textContent == null || textContent.trim().isEmpty()) {
                String warningMsg = "警告：抓取到的网页内容为空 - " + normalizedUrl;
                log.warn(warningMsg);
                return warningMsg;
            }

            // 截取前2000个字符（提供足够的上下文信息）
            int maxLength = 2000;
            String truncatedContent = textContent.substring(0, Math.min(textContent.length(), maxLength));

            log.info("成功抓取网页: {}, 纯文本长度: {} 字符（已截取至 {} 字符）",
                    normalizedUrl, textContent.length(), truncatedContent.length());

            return truncatedContent;

        } catch (IOException e) {
            // 捕获IO异常（网络错误、连接超时、404等）
            String errorMsg = String.format("网页抓取失败: %s, 错误: %s", normalizedUrl, e.getMessage());
            log.error(errorMsg, e);
            return errorMsg;
        } catch (Exception e) {
            // 捕获其他未预期的异常
            String errorMsg = String.format("网页抓取时发生未知错误: %s, 错误: %s", normalizedUrl, e.getMessage());
            log.error(errorMsg, e);
            return errorMsg;
        }
    }
}
