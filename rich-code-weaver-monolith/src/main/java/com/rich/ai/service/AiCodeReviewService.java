package com.rich.ai.service;

import com.rich.ai.model.CodeReviewResponse;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AI 代码审查服务接口
 * 系统提示词通过 systemMessageProvider 在工厂类中编程式指定（从数据库查询）
 *
 * @author DuRuiChi
 * @create 2026/1/14
 **/
public interface AiCodeReviewService {
    /**
     * 代码审查
     *
     * @param code 待审查代码
     * @return CodeReviewResponse
     **/
    @UserMessage("请审查以下代码：\n\n{{code}}")
    CodeReviewResponse codeReview(@V("code") String code);
}
