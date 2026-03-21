package com.rich.ai.service;

import com.rich.ai.model.CodeReviewResponse;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AI 代码审查服务接口
 *
 * @author DuRuiChi
 * @create 2025/9/14
 **/
public interface AiCodeReviewService {
    /**
     * 代码审查
     *
     * @param code 待审查代码
     * @return CodeReviewResponse
     **/
    @SystemMessage(fromResource = "aiPrompt/code-review-system-prompt.txt")
    @UserMessage("请审查以下代码：\n\n{{code}}")
    CodeReviewResponse codeReview(@V("code") String code);
}
