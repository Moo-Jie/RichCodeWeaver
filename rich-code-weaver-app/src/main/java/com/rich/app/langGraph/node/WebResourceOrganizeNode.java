package com.rich.app.langGraph.node;

import com.rich.ai.service.AiWebResourceOrganizeService;
import com.rich.app.langGraph.state.WorkflowContext;
import com.rich.common.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 网络资源整理节点（单独调用专精于网络资源整理的 AI 模型）
 *
 * @author DuRuiChi
 * @create 2026/1/11
 **/
@Slf4j
public class WebResourceOrganizeNode {
    /**
     * 异步创建图片资源获取节点
     *
     * @return 图片资源获取节点
     */
    public static AsyncNodeAction<MessagesState<String>> create() {
        // 返回一个异步节点,保存 state
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            long startTime = System.currentTimeMillis();
            log.info("\n========================================");
            log.info("[网络资源整理节点] 开始执行");
            log.info("产物ID: {}", context.getAppId());
            log.info("原始提示词: {}", context.getOriginalPrompt());
            log.info("========================================\n");
            
            // 单独调用专精于网络资源整理的 AI 模型服务获取网络资源
            try {
                AiWebResourceOrganizeService aiWebResourceOrganizeService = SpringContextUtil.getBean(AiWebResourceOrganizeService.class);
                log.info("[网络资源整理节点] 正在调用 AI 网络资源整理服务...");
                
                String webResourceListStr = aiWebResourceOrganizeService.webResource(context.getOriginalPrompt());
                
                // 录入工作流状态
                context.setWebResourceListStr(webResourceListStr);
                
                long duration = System.currentTimeMillis() - startTime;
                log.info("\n========================================");
                log.info("[网络资源整理节点] 执行成功");
                log.info("执行耗时: {} ms", duration);
                log.info("整理结果长度: {} 字符", webResourceListStr != null ? webResourceListStr.length() : 0);
                log.info("整理结果内容: {}", webResourceListStr != null && webResourceListStr.length() > 200 ? webResourceListStr.substring(0, 200) + "..." : webResourceListStr);
                log.info("========================================\n");
                
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                log.error("\n========================================");
                log.error("[网络资源整理节点] 执行失败");
                log.error("产物ID: {}", context.getAppId());
                log.error("执行耗时: {} ms", duration);
                log.error("异常类型: {}", e.getClass().getName());
                log.error("异常消息: {}", e.getMessage());
                log.error("详细堆栈信息:", e);
                log.error("========================================\n");
                
                context.setErrorMessage("网络资源整理节点运行失败: " + e.getMessage());
                return WorkflowContext.saveContext(context);
            }
            context.setCurrentStep("网络资源整理已完成");
            return WorkflowContext.saveContext(context);
        });
    }
}
