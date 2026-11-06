package com.rich.app.langGraph.node;

import cn.hutool.core.util.StrUtil;
import com.rich.ai.model.CodeReviewResponse;
import com.rich.app.langGraph.state.WorkflowContext;
import com.rich.model.enums.CodeGeneratorTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 交付门禁节点
 * 只允许通过结构检查、AI 审查和真实构建的产物进入可交付状态
 *
 * @author DuRuiChi
 * @create 2026/11/6
 **/
@Slf4j
public class DeliveryGateNode {

    /**
     * 创建交付门禁节点
     *
     * @return 异步节点
     */
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            CodeReviewResponse reviewResponse = context.getCodeReviewResponse();
            boolean reviewPassed = reviewResponse != null && Boolean.TRUE.equals(reviewResponse.getIsPass());
            boolean buildRequired = CodeGeneratorTypeEnum.VUE_PROJECT.equals(context.getGenerationType());
            boolean deliveryReady = context.isPreflightPassed() && reviewPassed
                    && (!buildRequired || context.isBuildPassed());

            context.setDeliveryReady(deliveryReady);
            if (deliveryReady) {
                context.setErrorMessage(null);
                context.setCurrentStep("交付质量门禁已通过");
                log.info("交付质量门禁通过: appId={}, type={}", context.getAppId(), context.getGenerationType());
            } else {
                List<String> failureReasons = collectFailureReasons(context, reviewResponse, buildRequired);
                context.setErrorMessage(String.join("\n", failureReasons));
                context.setCurrentStep("交付质量门禁未通过");
                log.warn("交付质量门禁未通过: appId={}, reasons={}", context.getAppId(), failureReasons);
            }
            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * 汇总所有阻断交付的原因
     *
     * @param context       工作流上下文
     * @param reviewResponse AI 审查结果
     * @param buildRequired 是否需要构建
     * @return 去重后的失败原因
     */
    private static List<String> collectFailureReasons(WorkflowContext context,
                                                       CodeReviewResponse reviewResponse,
                                                       boolean buildRequired) {
        Set<String> reasons = new LinkedHashSet<>();
        if (!context.isPreflightPassed()) {
            List<String> preflightErrors = context.getPreflightErrors();
            if (preflightErrors == null || preflightErrors.isEmpty()) {
                reasons.add("项目结构预检未通过");
            } else {
                reasons.addAll(preflightErrors);
            }
        }
        if (reviewResponse == null || !Boolean.TRUE.equals(reviewResponse.getIsPass())) {
            if (reviewResponse == null || reviewResponse.getErrorList() == null
                    || reviewResponse.getErrorList().isEmpty()) {
                reasons.add("AI 代码审查未通过或未产生有效结果");
            } else {
                reasons.addAll(reviewResponse.getErrorList());
            }
        }
        if (buildRequired && !context.isBuildPassed()) {
            String buildErrorLog = context.getBuildErrorLog();
            reasons.add(StrUtil.isBlank(buildErrorLog)
                    ? "Vue 项目未通过 npm run build"
                    : "Vue 项目构建失败:\n" + buildErrorLog);
        }
        return new ArrayList<>(reasons);
    }
}
