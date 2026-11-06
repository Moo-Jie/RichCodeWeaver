package com.rich.app.langGraph.node;

import com.rich.app.langGraph.state.WorkflowContext;
import com.rich.common.utils.SpringContextUtil;
import com.rich.common.utils.deployWebProjectUtils.BuildWebProjectExecutor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.File;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 项目构建节点
 * 使用真实 npm 构建结果驱动工作流路由，构建失败时保留日志用于下一轮修复
 *
 * @author DuRuiChi
 * @create 2026/1/11
 **/
@Slf4j
public class ProjectBuilderNode {

    /**
     * 创建项目构建节点
     *
     * @return 异步节点
     */
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("\n 正在执行节点: 项目构建。\n");

            BuildWebProjectExecutor buildWebProjectExecutor =
                    SpringContextUtil.getBean(BuildWebProjectExecutor.class);
            BuildWebProjectExecutor.BuildResult buildResult =
                    buildWebProjectExecutor.buildProjectWithLog(context.getOutputDir());

            context.setBuildPassed(buildResult.success());
            if (buildResult.success()) {
                context.setBuildErrorLog(null);
                context.setDeployDir(context.getOutputDir() + File.separator + "dist");
                context.setCurrentStep("Vue 项目构建已通过");
                log.info("Vue 项目构建成功: appId={}, dist={}", context.getAppId(), context.getDeployDir());
            } else {
                context.setDeployDir(null);
                context.setBuildErrorLog(buildResult.log());
                context.setRepairAttemptCount(context.getRepairAttemptCount() + 1);
                context.setCurrentStep("Vue 项目构建未通过");
                log.warn("Vue 项目构建失败: appId={}, phase={}, log={}",
                        context.getAppId(), buildResult.phase(), buildResult.log());
            }
            return WorkflowContext.saveContext(context);
        });
    }
}
