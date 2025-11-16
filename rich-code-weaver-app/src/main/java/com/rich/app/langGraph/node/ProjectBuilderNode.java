package com.rich.app.langGraph.node;

import com.rich.app.langGraph.state.WorkflowContext;
import com.rich.app.utils.deployWebProjectUtils.BuildWebProjectExecutor;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.common.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.File;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 项目构建节点
 *
 * @author DuRuiChi
 * @create 2025/9/11
 **/
@Slf4j
public class ProjectBuilderNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("\n 正在执行节点: 项目构建。\n");

            String outputDir = context.getOutputDir();
            String buildResultDir;
            try {
                BuildWebProjectExecutor buildWebProjectExecutor = SpringContextUtil.getBean(BuildWebProjectExecutor.class);
                // 构建代码
                boolean buildSuccess = buildWebProjectExecutor.buildProject(outputDir);
                if (buildSuccess) {
                    // 返回 dist 目录的路径
                    buildResultDir = outputDir + File.separator + "dist";
                    log.info("项目构建成功，目录为: {}", buildResultDir);
                } else {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "工程项目构建失败");
                }
            } catch (Exception e) {
                log.error("Vue 项目构建异常: {}", e.getMessage(), e);
                // 返回原路径
                buildResultDir = outputDir;
            }

            // 更新状态
            context.setDeployDir(buildResultDir);
            context.setCurrentStep("项目构建已完成");
            log.info("\n 项目构建节点运行完成，最终目录: {} \n ", buildResultDir);
            return WorkflowContext.saveContext(context);
        });
    }
}
