package com.rich.app.langGraph.node;

import com.rich.ai.service.AiImageResourceService;
import com.rich.app.langGraph.state.WorkflowContext;
import com.rich.common.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 更智能、准确的图片资源收集节点（单独调用专精于图片收集的 AI 模型）
 *
 * @author DuRuiChi
 * @create 2025/9/11
 **/
@Slf4j
public class ImageResourceNode {
    /**
     * 异步创建图片资源获取节点
     *
     * @return 图片资源获取节点
     */
    public static AsyncNodeAction<MessagesState<String>> create() {
        // 返回一个异步节点，保存 state
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("\n 正在执行节点: 图片收集。\n");
            // 单独调用专精于图片收集的 AI 模型服务获取图片资源
            try {
                AiImageResourceService aiImageResourceService = SpringContextUtil.getBean(AiImageResourceService.class);
                String imageListStr = aiImageResourceService.resourceImages(context.getOriginalPrompt());
                // 录入工作流状态
                context.setImageListStr(imageListStr);
            } catch (Exception e) {
                log.error("图片收集节点运行失败", e);
                context.setErrorMessage("图片收集节点运行失败");
                return WorkflowContext.saveContext(context);
            }
            context.setCurrentStep("图片收集已完成");
            log.info("\n 图片收集节点运行完成。\n");
            return WorkflowContext.saveContext(context);
        });
    }
}
