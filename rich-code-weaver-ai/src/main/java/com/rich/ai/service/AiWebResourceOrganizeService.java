package com.rich.ai.service;

import dev.langchain4j.service.UserMessage;

/**
 * AI 网络资源整理服务接口
 * 系统提示词通过 systemMessageProvider 在工厂类中编程式指定（从数据库查询）
 *
 * @author DuRuiChi
 * @create 2026/1/14
 **/
public interface AiWebResourceOrganizeService {
    /**
     * 根据用户提示词收集所需的网络资源
     *
     * @param userPrompt 用户提示词
     * @return List<ImageResource>
     **/
    String webResource(@UserMessage String userPrompt);
}
