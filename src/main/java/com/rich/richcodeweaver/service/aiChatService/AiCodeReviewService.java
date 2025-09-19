package com.rich.richcodeweaver.service.aiChatService;

import com.rich.richcodeweaver.model.aiChatResponse.CodeReviewResponse;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

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
    CodeReviewResponse codeReview(@UserMessage String code);
}
