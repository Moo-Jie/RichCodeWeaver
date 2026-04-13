package com.rich.codeweaver.service.generator;

import com.rich.codeweaver.model.dto.generator.codeResponse.HtmlCodeResponse;
import com.rich.codeweaver.model.dto.generator.codeResponse.MultiFileCodeResponse;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import reactor.core.publisher.Flux;

/**
 * AI 代码生成服务接口
 * 系统提示词通过 systemMessageProvider 在工厂类中编程式指定（从数据库查询）
 *
 * @author DuRuiChi
 * @create 2025/12/5
 **/
public interface AiCodeGeneratorService {
    /**
     * AI 流式生成 HTML 代码
     *
     * @param userMessage 用户提示词
     * @return AI 的输出结果
     */
    @UserMessage("{{userMessage}}")
    Flux<String> generateHtmlCodeStream(@V("userMessage") String userMessage);

    /**
     * AI 流式生成多文件代码
     *
     * @param userMessage 用户提示词
     * @return AI 的输出结果
     */
    @UserMessage("{{userMessage}}")
    Flux<String> generateMultiFileCodeStream(@V("userMessage") String userMessage);

    /**
     * AI 流式推理生成 Vue 项目工程代码
     *
     * @param userMessage 用户提示词
     * @param appId       产物 ID 作为 MemoryId ,用于让 AI 主动唯一标识正在生成的产物
     * @return AI 的输出结果
     */
    @UserMessage("{{userMessage}}")
    TokenStream generateVueProjectCodeStream(@V("userMessage") String userMessage, @MemoryId Long appId);

    /**
     * AI 生成 HTML 代码
     *
     * @param userMessage 用户提示词
     * @return AI 的输出结果
     */
    @UserMessage("{{userMessage}}")
    HtmlCodeResponse generateHtmlCode(@V("userMessage") String userMessage);

    /**
     * AI 生成多文件代码
     *
     * @param userMessage 用户提示词
     * @return AI 的输出结果
     */
    @UserMessage("{{userMessage}}")
    MultiFileCodeResponse generateMultiFileCode(@V("userMessage") String userMessage);

    /**
     * AI 生成 Vue 项目工程代码
     *
     * @param userMessage 用户提示词
     * @param appId       产物 ID 作为 MemoryId ,用于让 AI 主动唯一标识正在生成的产物
     * @return AI 的输出结果
     */
    @UserMessage("{{userMessage}}")
    MultiFileCodeResponse generateVueProjectCode(@V("userMessage") String userMessage, @MemoryId Long appId);
}
