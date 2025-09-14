package com.rich.richcodeweaver.langGraph.node;

import com.rich.richcodeweaver.exception.ErrorCode;
import com.rich.richcodeweaver.exception.ThrowUtils;
import com.rich.richcodeweaver.langGraph.state.WorkflowContext;
import com.rich.richcodeweaver.model.enums.CodeGeneratorTypeEnum;
import com.rich.richcodeweaver.service.aiChatService.AiCodeGeneratorTypeStrategyService;
import com.rich.richcodeweaver.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 代码生成类型策略节点
 *
 * @author DuRuiChi
 * @create 2025/9/11
 **/
@Slf4j
public class AiCodeGeneratorTypeStrategyNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            CodeGeneratorTypeEnum generationType = context.getGenerationType();
            log.info("\n 正在执行节点: 代码生成类型策略节点。\n");
            ThrowUtils.throwIf(generationType == null, ErrorCode.PARAMS_ERROR, "代码生成类型不能为空");
            // 智能路由
            try {
                if (CodeGeneratorTypeEnum.AI_STRATEGY.equals(generationType)) {
                    // 获取AI路由服务
                    AiCodeGeneratorTypeStrategyService aiCodeGeneratorTypeStrategyService = SpringContextUtil.getBean(AiCodeGeneratorTypeStrategyService.class);
                    // 根据原始提示词进行智能路由
                    CodeGeneratorTypeEnum genType = aiCodeGeneratorTypeStrategyService.getCodeGenStrategy(context.getOriginalPrompt());
                    log.info("AI 智能选择代码生成方案: {} ({})", genType.getValue(), genType.getText());
                } else {
                    log.info("直接使用用户指定的代码生成方案: {}", generationType.getValue());
                }
            } catch (Exception e) {
                log.error("AI 智能路由失败，使用默认 HTML 类型: {}", e.getMessage());
                generationType = CodeGeneratorTypeEnum.HTML;
            }

            // 状态更新
            context.setCurrentStep("代码生成类型策略已完成");
            context.setGenerationType(generationType);
            log.info("\n 代码生成类型策略节点运行完成。\n");
            return WorkflowContext.saveContext(context);
        });
    }
}
