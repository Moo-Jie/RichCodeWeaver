package com.rich.richcodeweaver.langGraph.node;

import cn.hutool.core.util.StrUtil;
import com.rich.richcodeweaver.langGraph.state.WorkflowContext;
import com.rich.richcodeweaver.model.aiChatResponse.CodeReviewResponse;
import com.rich.richcodeweaver.service.aiChatService.AiCodeReviewService;
import com.rich.richcodeweaver.utils.SpringContextUtil;
import com.rich.richcodeweaver.utils.aiUtils.codeConcatenate.CodeConcatenateUtiles;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 代码审查节点
 *
 * @author DuRuiChi
 * @create 2025/9/18
 **/
@Slf4j
public class AICodeReviewNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("\n 正在执行节点: 代码审查节点。 \n");
            String outputDir = context.getOutputDir();
            CodeReviewResponse codeReviewResponse;
            try {
                // 读取并拼接代码文件内容
                String codeContent = CodeConcatenateUtiles.readAndConcatenateCodeFiles(outputDir);
                if (StrUtil.isBlank(codeContent)) {
                    log.warn("未找到可审查的代码文件");
                    codeReviewResponse = CodeReviewResponse.builder()
                            .isPass(false)
                            .errorList(List.of("未找到可审查的代码文件"))
                            .suggestionList(List.of("请确保代码生成成功"))
                            .build();
                } else {
                    // 代码审查 AI 服务
                    AiCodeReviewService aiCodeReviewService = SpringContextUtil.getBean(AiCodeReviewService.class);
                    codeReviewResponse = aiCodeReviewService.codeReview(codeContent);
                    if (codeReviewResponse.getIsPass()) {
                        log.info("\n 代码审查通过。\n");
                    } else {
                        log.info("\n 代码审查未通过,错误列表: {}\n", codeReviewResponse.getErrorList());
                    }
                }
            } catch (Exception e) {
                log.error("代码审查异常: {}", e.getMessage(), e);
                codeReviewResponse = CodeReviewResponse.builder()
                        .isPass(true) // 异常直接跳到下一个步骤
                        .build();
            }
            // 3. 更新状态
            Long currentCount = codeReviewResponse.getReviewCount();
            codeReviewResponse.setReviewCount(currentCount != null ? currentCount + 1 : 1L);
            context.setCurrentStep("代码审查");
            context.setCodeReviewResponse(codeReviewResponse);
            log.info("\n 代码审查节点运行完成。\n");
            return WorkflowContext.saveContext(context);
        });
    }
}
