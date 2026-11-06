package com.rich.app.langGraph.node;

import com.rich.app.langGraph.state.WorkflowContext;
import com.rich.app.quality.ProjectQualityGateService;
import com.rich.common.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 项目结构预检节点
 * 在调用 AI 审查和 npm 构建前执行确定性检查，拦截明显无法构建的产物
 *
 * @author DuRuiChi
 * @create 2026/11/6
 **/
@Slf4j
public class ProjectPreflightNode {

    /**
     * 创建项目结构预检节点
     *
     * @return 异步节点
     */
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("\n 正在执行节点: 项目结构预检。\n");

            ProjectQualityGateService qualityGateService =
                    SpringContextUtil.getBean(ProjectQualityGateService.class);
            ProjectQualityGateService.ValidationResult validationResult =
                    qualityGateService.validate(context.getOutputDir(), context.getGenerationType());

            context.setPreflightPassed(validationResult.passed());
            context.setPreflightErrors(validationResult.errors());
            if (!validationResult.passed()) {
                context.setRepairAttemptCount(context.getRepairAttemptCount() + 1);
                context.setCurrentStep("项目结构预检未通过");
                log.warn("项目结构预检未通过: appId={}, errors={}", context.getAppId(), validationResult.errors());
            } else {
                context.setCurrentStep("项目结构预检已通过");
            }
            return WorkflowContext.saveContext(context);
        });
    }
}
