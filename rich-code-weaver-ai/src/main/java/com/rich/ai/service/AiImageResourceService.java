package com.rich.ai.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * AI 图片生成服务接口
 *
 * @author DuRuiChi
 * @create 2025/9/14
 **/
public interface AiImageResourceService {
    /**
     * 根据用户提示词收集所需的图片资源
     *
     * @param userPrompt 用户提示词
     * @return List<ImageResource>
     **/
    @SystemMessage(fromResource = "aiPrompt/image-resource-system-prompt.txt")
    String resourceImages(@UserMessage String userPrompt);
}
