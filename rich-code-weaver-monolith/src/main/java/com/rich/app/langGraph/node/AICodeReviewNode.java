package com.rich.app.langGraph.node;

import com.rich.ai.model.CodeReviewResponse;
import com.rich.ai.service.AiCodeReviewService;
import com.rich.app.langGraph.state.WorkflowContext;
import com.rich.app.utils.codeConcatenate.CodeConcatenateUtiles;
import com.rich.common.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 代码审查节点
 * 只负责 AI 语义审查，项目结构和构建结果由独立质量门禁节点负责
 *
 * @author DuRuiChi
 * @create 2026/1/18
 **/
@Slf4j
public class AICodeReviewNode {

    /**
     * 创建代码审查节点
     *
     * @return 异步节点
     */
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("\n 正在执行节点: 代码审查节点。\n");

            CodeReviewResponse codeReviewResponse;
            try {
                if (!context.isPreflightPassed()) {
                    codeReviewResponse = CodeReviewResponse.builder()
                            .isPass(false)
                            .errorList(List.of("确定性结构检查未通过，跳过 AI 审查"))
                            .build();
                } else {
                    String codeContent = CodeConcatenateUtiles.readAndConcatenateCodeFiles(context.getOutputDir());
                    if (codeContent.isBlank()) {
                        codeReviewResponse = CodeReviewResponse.builder()
                                .isPass(false)
                                .errorList(List.of("未找到可审查的代码文件"))
                                .suggestionList(List.of("请确保代码生成成功"))
                                .build();
                    } else {
                        AiCodeReviewService aiCodeReviewService = SpringContextUtil.getBean(AiCodeReviewService.class);
                        codeReviewResponse = aiCodeReviewService.codeReview(codeContent);
                    }
                }
            } catch (Exception e) {
                // 审查服务异常必须失败关闭，否则会把未审查代码当成合格产物
                log.error("代码审查异常，按未通过处理: appId={}", context.getAppId(), e);
                codeReviewResponse = CodeReviewResponse.builder()
                        .isPass(false)
                        .errorList(List.of("AI 代码审查执行异常: " + e.getMessage()))
                        .suggestionList(List.of("检查审查模型配置和上下文长度，修复后重新执行质量门禁"))
                        .build();
            }

            if (codeReviewResponse == null || codeReviewResponse.getIsPass() == null) {
                codeReviewResponse = CodeReviewResponse.builder()
                        .isPass(false)
                        .errorList(List.of("代码审查未返回有效结果"))
                        .build();
            }
            if (!Boolean.TRUE.equals(codeReviewResponse.getIsPass())) {
                context.setRepairAttemptCount(context.getRepairAttemptCount() + 1);
            }
            codeReviewResponse.setReviewCount(codeReviewResponse.getReviewCount() == null
                    ? 1L
                    : codeReviewResponse.getReviewCount() + 1);
            context.setCurrentStep(Boolean.TRUE.equals(codeReviewResponse.getIsPass())
                    ? "代码审查已通过"
                    : "代码审查未通过");
            context.setCodeReviewResponse(codeReviewResponse);
            log.info("代码审查节点运行完成: appId={}, passed={}",
                    context.getAppId(), codeReviewResponse.getIsPass());
            return WorkflowContext.saveContext(context);
        });
    }
}
