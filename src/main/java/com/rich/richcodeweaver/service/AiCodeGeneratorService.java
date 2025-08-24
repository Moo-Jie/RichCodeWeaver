package com.rich.richcodeweaver.service;

import com.rich.richcodeweaver.model.dto.aiCode.HtmlCodeResult;
import com.rich.richcodeweaver.model.dto.aiCode.MultiFileCodeResult;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import reactor.core.publisher.Flux;

/**
 * AI 代码生成服务接口
 *
 * @author DuRuiChi
 * @create 2025/8/5
 **/
public interface AiCodeGeneratorService {
    /**
     * AI 流式生成 HTML 代码
     *
     * @param userMessage 用户提示词
     * @return AI 的输出结果
     */
    @SystemMessage(fromResource = "/aiPrompt/html-system-prompt.txt")
    Flux<String> generateHtmlCodeStream(String userMessage);

    /**
     * AI 流式生成多文件代码
     *
     * @param userMessage 用户提示词
     * @return AI 的输出结果
     */
    @SystemMessage(fromResource = "/aiPrompt/multi-file-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(String userMessage);

    /**
     * AI 流式推理生成 Vue 项目工程代码
     *
     * @param userMessage 用户提示词
     * @param appId 应用 ID 作为 MemoryId ,用于让 AI 主动唯一标识正在生成的应用
     * @return AI 的输出结果
     */
    @SystemMessage(fromResource = "/aiPrompt/vue-project-system-prompt.txt")
    Flux<String> generateVueProjectCodeStream(String userMessage, @MemoryId Long appId);

    /**
     * AI 生成 HTML 代码
     *
     * @param userMessage 用户提示词
     * @return AI 的输出结果
     */
    @SystemMessage(fromResource = "/aiPrompt/html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(String userMessage);

    /**
     * AI 生成多文件代码
     *
     * @param userMessage 用户提示词
     * @return AI 的输出结果
     */
    @SystemMessage(fromResource = "/aiPrompt/multi-file-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);
}
