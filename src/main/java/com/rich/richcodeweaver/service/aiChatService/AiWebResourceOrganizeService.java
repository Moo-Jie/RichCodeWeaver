package com.rich.richcodeweaver.service.aiChatService;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * AI 网络资源整理服务接口
 *
 * @author DuRuiChi
 * @create 2025/9/14
 **/
public interface AiWebResourceOrganizeService {
    /**
     * 根据用户提示词收集所需的网络资源
     *
     * @param userPrompt 用户提示词
     * @return List<ImageResource>
     **/
    @SystemMessage(fromResource = "aiPrompt/web-resource-organize-system-prompt.txt")
    String webResource(@UserMessage String userPrompt);
}
