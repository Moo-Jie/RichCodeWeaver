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
 * @create 2026/1/11
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
            long startTime = System.currentTimeMillis();
            log.info("\n========================================");
            log.info("[图片收集节点] 开始执行");
            log.info("产物ID: {}", context.getAppId());
            log.info("原始提示词: {}", context.getOriginalPrompt());
            log.info("========================================\n");

            // 单独调用专精于图片收集的 AI 模型服务获取图片资源
            try {
                AiImageResourceService aiImageResourceService = SpringContextUtil.getBean(AiImageResourceService.class);
                log.info("[图片收集节点] 正在调用 AI 图片资源服务...");

                String imageListStr = aiImageResourceService.resourceImages(context.getOriginalPrompt());

                // 录入工作流状态
                context.setImageListStr(imageListStr);

                long duration = System.currentTimeMillis() - startTime;
                log.info("\n========================================");
                log.info("[图片收集节点] 执行成功");
                log.info("执行耗时: {} ms", duration);
                log.info("收集结果长度: {} 字符", imageListStr != null ? imageListStr.length() : 0);
                log.info("收集结果内容: {}", imageListStr != null && imageListStr.length() > 200 ? imageListStr.substring(0, 200) + "..." : imageListStr);
                log.info("========================================\n");

            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                log.error("\n========================================");
                log.error("[图片收集节点] 执行失败");
                log.error("产物ID: {}", context.getAppId());
                log.error("执行耗时: {} ms", duration);
                log.error("异常类型: {}", e.getClass().getName());
                log.error("异常消息: {}", e.getMessage());
                log.error("详细堆栈信息:", e);
                log.error("========================================\n");

                context.setErrorMessage("图片收集节点运行失败: " + e.getMessage());
                return WorkflowContext.saveContext(context);
            }
            context.setCurrentStep("图片收集已完成");
            return WorkflowContext.saveContext(context);
        });
    }
}
