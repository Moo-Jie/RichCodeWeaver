package com.rich.richcodeweaver.langGraph.state;

import com.rich.richcodeweaver.model.entity.ImageResource;
import com.rich.richcodeweaver.model.enums.CodeGeneratorTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 工作流上下文，用于存储工作流的状态信息（保存所有工作流节点执行完毕后的信息）
 *
 * @author DuRuiChi
 * @create 2025/9/8
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowContext implements Serializable {

    /**
     * WorkflowContext 在 MessagesState 中的存储 key
     */
    public static final String WORKFLOW_CONTEXT_KEY = "workflowContext";
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 应用 ID
     */
    private Long appId;
    /**
     * 当前执行步骤
     */
    private String currentStep;
    /**
     * 用户原始输入的提示词
     */
    private String originalPrompt;
    /**
     * 网络资源字符串
     */
    private String webResourceListStr;
    /**
     * 图片资源字符串
     */
    private String imageListStr;
    /**
     * 代码生成类型
     **/
    private CodeGeneratorTypeEnum codeGenType;
    /**
     * 图片资源列表
     */
    private List<ImageResource> imageList;
    /**
     * 增强后的提示词
     */
    private String enhancedPrompt;
    /**
     * 代码生成类型
     */
    private CodeGeneratorTypeEnum generationType;
    /**
     * 生成的代码目录
     */
    private String outputDir;
    /**
     * 构建成功的目录
     */
    private String deployDir;
    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 从 MessagesState 中获取 WorkflowContext
     */
    public static WorkflowContext getContext(MessagesState<String> state) {
        return (WorkflowContext) state.data().get(WORKFLOW_CONTEXT_KEY);
    }

    /**
     * 将 WorkflowContext 保存到 MessagesState 中
     */
    public static Map<String, Object> saveContext(WorkflowContext context) {
        return Map.of(WORKFLOW_CONTEXT_KEY, context);
    }
}
