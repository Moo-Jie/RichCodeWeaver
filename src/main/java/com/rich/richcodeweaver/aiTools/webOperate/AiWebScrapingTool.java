package com.rich.richcodeweaver.aiTools.webOperate;

import cn.hutool.json.JSONObject;
import com.rich.richcodeweaver.aiTools.BaseTool;
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
 * @create 2025/7/6
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
        // 超长截取
        String scrapUrl = arguments.getStr("ScrapUrl");
        if(scrapUrl.length() > 100) {
            scrapUrl = scrapUrl.substring(0, 100)+"(超长省略...)";
        }
        scrapUrl = scrapUrl == null || scrapUrl.isEmpty() ? "" : "\n[\n" + scrapUrl + "\n]\n";
        return String.format("[工具调用结束] %s %s", "成功抓取以下网页内容", scrapUrl);
    }

    @Tool("抓取网页内容")
    public String scrapeWebPage(@P("要抓取的网页的 URL") String ScrapUrl) {
        try {
            String doc = Jsoup.connect(ScrapUrl).get().html();
            return doc.substring(0, Math.min(doc.length(), 400));
        } catch (IOException e) {
            return "网页 " + ScrapUrl + " 抓取失败: " + e.getMessage();
        }
    }
}
