package com.rich.app.langGraph.node;

import com.rich.ai.service.AiImageResourceService;
import com.rich.ai.service.AiWebResourceOrganizeService;
import com.rich.app.langGraph.state.WorkflowContext;
import com.rich.common.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.concurrent.CompletableFuture;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 资源收集合并节点：并行执行网络资源整理和图片资源收集
 * 替代原来串行的 WebResourceOrganizeNode → ImageResourceNode，节省约 19 秒
 *
 * @author DuRuiChi
 * @create 2026/3/27
 **/
@Slf4j
public class ResourceCollectorNode {
    /**
     * 异步创建资源收集合并节点
     * 内部使用 CompletableFuture.allOf() 并行执行网络资源整理和图片资源收集
     *
     * @return 资源收集合并节点
     */
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            long startTime = System.currentTimeMillis();
            log.info("\n========================================");
            log.info("[资源收集节点] 开始并行执行（网络资源 + 图片资源）");
            log.info("产物ID: {}", context.getAppId());
            log.info("原始提示词: {}", context.getOriginalPrompt());
            log.info("========================================\n");

            String originalPrompt = context.getOriginalPrompt();

            // 检测用户是否已提供素材（由 MaterialService.formatMaterialsForPrompt 生成的标记）
            // 若已提供素材，跳过全部资源收集，直接使用用户素材，避免产生两套互相冲突的"必须使用"指令
            if (originalPrompt.contains("【必须使用的素材资源】")) {
                log.info("[资源收集节点] 检测到用户已提供素材，跳过资源收集，直接使用用户素材");
                context.setWebResourceListStr("无");
                context.setImageListStr("无");
                context.setCurrentStep("资源收集已跳过（用户已提供素材，优先使用用户素材）");
                return WorkflowContext.saveContext(context);
            }

            // 并行执行：网络资源整理 + 图片资源收集
            CompletableFuture<String> webFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("[资源收集节点] 正在并行调用 AI 网络资源整理服务...");
                    AiWebResourceOrganizeService aiWebResourceOrganizeService =
                            SpringContextUtil.getBean(AiWebResourceOrganizeService.class);
                    return aiWebResourceOrganizeService.webResource(originalPrompt);
                } catch (Exception e) {
                    log.error("[资源收集节点] 网络资源整理失败: {}", e.getMessage(), e);
                    return null;
                }
            });

            CompletableFuture<String> imageFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("[资源收集节点] 正在并行调用 AI 图片资源服务...");
                    AiImageResourceService aiImageResourceService =
                            SpringContextUtil.getBean(AiImageResourceService.class);
                    return aiImageResourceService.resourceImages(originalPrompt);
                } catch (Exception e) {
                    log.error("[资源收集节点] 图片资源收集失败: {}", e.getMessage(), e);
                    return null;
                }
            });

            // 等待两个任务都完成
            try {
                CompletableFuture.allOf(webFuture, imageFuture).join();
            } catch (Exception e) {
                log.error("[资源收集节点] 并行任务等待异常: {}", e.getMessage(), e);
            }

            // 收集结果
            String webResourceListStr = webFuture.getNow(null);
            String imageListStr = imageFuture.getNow(null);

            // 设置网络资源结果
            if (webResourceListStr != null) {
                context.setWebResourceListStr(webResourceListStr);
                log.info("[资源收集节点] 网络资源整理完成，长度: {} 字符", webResourceListStr.length());
            } else {
                log.warn("[资源收集节点] 网络资源整理未获取到结果");
            }

            // 设置图片资源结果
            if (imageListStr != null) {
                context.setImageListStr(imageListStr);
                log.info("[资源收集节点] 图片资源收集完成，长度: {} 字符", imageListStr.length());
            } else {
                log.warn("[资源收集节点] 图片资源收集未获取到结果");
            }

            // 合并错误信息
            StringBuilder errorMessages = new StringBuilder();
            if (webResourceListStr == null) {
                errorMessages.append("网络资源整理失败; ");
            }
            if (imageListStr == null) {
                errorMessages.append("图片资源收集失败; ");
            }
            if (errorMessages.length() > 0) {
                context.setErrorMessage("资源收集节点部分失败: " + errorMessages);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("\n========================================");
            log.info("[资源收集节点] 并行执行完成");
            log.info("总耗时: {} ms", duration);
            log.info("网络资源: {} 字符", webResourceListStr != null ? webResourceListStr.length() : 0);
            log.info("图片资源: {} 字符", imageListStr != null ? imageListStr.length() : 0);
            log.info("========================================\n");

            context.setCurrentStep("资源收集已完成（网络资源 + 图片资源并行）");
            return WorkflowContext.saveContext(context);
        });
    }
}
