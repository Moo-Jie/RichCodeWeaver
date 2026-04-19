package com.rich.codeweaver.langGraph.state;

import com.rich.codeweaver.monitor.MonitorContext;
import com.rich.codeweaver.model.dto.generator.CodeReviewResponse;
import com.rich.codeweaver.model.entity.App;
import com.rich.codeweaver.model.entity.ImageResource;
import com.rich.codeweaver.model.enums.CodeGeneratorTypeEnum;
import com.rich.codeweaver.service.generator.AppService;
import com.rich.codeweaver.common.utils.SpringContextUtil;
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
 * @create 2026/1/8
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
     * 产物 ID
     */
    private Long appId;
    /**
     * 用户 ID
     */
    private Long userId;
    /**
     * 生成模式
     */
    private String genMode;
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
     * 代码审查结果
     */
    private CodeReviewResponse codeReviewResponse;
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
     * 是否为二次修改模式（非首次生成）
     */
    private boolean isModification;

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

    public MonitorContext toMonitorContext() {
        if (userId == null || appId == null || genMode == null || genMode.isBlank()) {
            return null;
        }
        return MonitorContext.builder()
                .userId(userId.toString())
                .appId(appId.toString())
                .genMode(genMode)
                .build();
    }

    /**
     * 更新数据库中的 app codeGenType 字段
     * 用于 AI 判断后将 AI_STRATEGY 转换为实际的生成类型（HTML/MULTI_FILE/VUE_PROJECT）
     */
    public void updateAppCodeGenType(CodeGeneratorTypeEnum finalType) {
        if (this.appId == null || finalType == null) {
            return;
        }
        try {
            AppService appService = SpringContextUtil.getBean(AppService.class);
            App app = new App();
            app.setId(this.appId);
            app.setCodeGenType(finalType.getValue());
            appService.updateById(app);
        } catch (Exception e) {
            // 记录日志但不中断流程
            System.err.println("更新 app codeGenType 失败: " + e.getMessage());
        }
    }
}
