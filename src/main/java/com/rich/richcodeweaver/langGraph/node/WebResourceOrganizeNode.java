package com.rich.richcodeweaver.langGraph.node;

import com.rich.richcodeweaver.langGraph.state.WorkflowContext;
import com.rich.richcodeweaver.service.aiChatService.AiCodeGeneratorTypeStrategyService;
import com.rich.richcodeweaver.service.aiChatService.AiImageResourceService;
import com.rich.richcodeweaver.service.aiChatService.AiWebResourceOrganizeService;
import com.rich.richcodeweaver.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 网络资源整理节点（单独调用专精于网络资源整理的 AI 模型）
 *
 * @author DuRuiChi
 * @create 2025/9/11
 **/
@Slf4j
public class WebResourceOrganizeNode {
    /**
     * 异步创建图片资源获取节点
     *
     * @return 图片资源获取节点
     */
    public static AsyncNodeAction<MessagesState<String>> create() {
        // 返回一个异步节点，保存 state
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("\n 正在执行节点: 网络资源整理。\n");
            // 单独调用专精于网络资源整理的 AI 模型服务获取网络资源
            try {
                AiWebResourceOrganizeService aiWebResourceOrganizeService = SpringContextUtil.getBean(AiWebResourceOrganizeService.class);
                String webResourceListStr = aiWebResourceOrganizeService.webResource(context.getOriginalPrompt());
                // 录入工作流状态
                context.setWebResourceListStr(webResourceListStr);
            } catch (Exception e) {
                log.error("网络资源整理节点运行失败", e);
                context.setErrorMessage("网络资源整理节点运行失败");
                return WorkflowContext.saveContext(context);
            }
            context.setCurrentStep("网络资源整理已完成");
            log.info("\n 网络资源整理节点运行完成。\n");
            return WorkflowContext.saveContext(context);
        });
    }
}
