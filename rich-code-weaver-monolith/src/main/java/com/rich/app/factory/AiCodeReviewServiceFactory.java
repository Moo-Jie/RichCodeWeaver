package com.rich.app.factory;

import com.rich.ai.service.AiCodeReviewService;
import com.rich.prompt.api.InnerSystemPromptService;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 代码审查服务实例创建工厂
 * 负责创建用于代码审查的 AI 服务实例
 *
 * @author DuRuiChi
 * @since 2025-09-05
 */
@Slf4j
@Configuration
public class AiCodeReviewServiceFactory {

    /**
     * 代码级审查契约，覆盖后台历史提示词中的宽松判断规则
     */
    private static final String CODE_REVIEW_RUNTIME_CONTRACT = """

## 运行时审查契约（优先级最高）

- 当前版本必须失败关闭：不能确认代码可运行时，isPass 必须为 false，禁止“不确定时倾向通过”。
- 重点检查模板语法、import 路径、组件注册、路由、未定义变量、依赖声明和静态资源引用。
- errorList 必须给出可定位的文件、代码片段或问题类型；不要只写“建议优化”。
- 结构检查和 npm run build 由独立门禁执行，但你不能因此放宽代码语义审查。
""";

    /**
     * 多例模式下的代码审查 AI 服务模型
     **/
    @Resource(name = "codeReviewChatModel")
    private ChatModel codeReviewChatModel;

    /**
     * 系统提示词内部服务（本地 Bean）
     **/
    @Resource
    private InnerSystemPromptService innerSystemPromptService;

    /**
     * 创建 AI 代码审查服务实例
     * 使用专用的代码审查模型构建 AI 服务，用于对生成的代码进行质量审查
     *
     * @return AiCodeReviewService AI 代码审查服务实例
     * @author DuRuiChi
     */
    @Bean
    public AiCodeReviewService aiCodeReviewService() {
        String systemPromptContent = innerSystemPromptService.getPromptContentByKey("code-review-system-prompt");
        if (systemPromptContent == null || systemPromptContent.isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "未找到系统提示词，promptKey=code-review-system-prompt，请在管理后台配置");
        }
        return AiServices.builder(AiCodeReviewService.class)
                .chatModel(codeReviewChatModel)
                .systemMessageProvider(memoryId -> systemPromptContent + CODE_REVIEW_RUNTIME_CONTRACT)
                // TODO 配置提示词护轨规则，目前规则不够完善，容易导致误判
//                .inputGuardrails(new PromptSafetyInputGuardrail())
                .build();
    }
}
