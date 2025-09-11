package com.rich.richcodeweaver.langgraph4j.node;

import com.rich.richcodeweaver.langgraph4j.state.WorkflowContext;
import com.rich.richcodeweaver.model.entity.ImageResource;
import com.rich.richcodeweaver.model.enums.ImageCategoryEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.Arrays;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 图片收集节点
 *
 * @author DuRuiChi
 * @create 2025/9/11
 **/
@Slf4j
public class ImageCollectorNode {
    /**
     * 异步创建代码生成节点
     *
     * @return 代码生成节点
     */
    public static AsyncNodeAction<MessagesState<String>> create() {
        // 返回一个异步节点，保存 state
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("\n 正在执行节点: 图片收集。\n");

            // TODO 实现逻辑

            // TODO 模拟工作流上下文状态内容的更新
            List<ImageResource> imageList = Arrays.asList(
                    ImageResource.builder()
                            .category(ImageCategoryEnum.CONTENT)
                            .description("图片01")
                            .url("https://picsum.photos/400/300?random=1")
                            .build(),
                    ImageResource.builder()
                            .category(ImageCategoryEnum.LOGO)
                            .description("图片02")
                            .url("https://picsum.photos/400/300?random=2")
                            .build()
            );
            context.setCurrentStep("图片收集已完成");
            context.setImageList(imageList);
            log.info("\n 图片收集节点运行完成。\n");
            return WorkflowContext.saveContext(context);
        });
    }
}
