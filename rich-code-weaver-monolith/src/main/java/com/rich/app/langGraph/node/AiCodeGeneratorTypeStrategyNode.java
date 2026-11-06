package com.rich.app.langGraph.node;

import com.rich.ai.service.AiCodeGeneratorTypeStrategyService;
import com.rich.app.langGraph.state.WorkflowContext;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.common.utils.SpringContextUtil;
import com.rich.model.enums.CodeGeneratorTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.regex.Pattern;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 代码生成类型策略节点
 *
 * @author DuRuiChi
 * @create 2026/1/11
 **/
@Slf4j
public class AiCodeGeneratorTypeStrategyNode {

    /**
     * 用户提示词中明确指定 VUE 项目模式的关键词正则（不区分大小写）
     */
    private static final Pattern VUE_PROJECT_PATTERN = Pattern.compile(
            "(?i)(vue\\s*项目|vue\\s*3?\\s*工程|vue\\s*project|组件化开发|SPA产物|单页产物框架|vue\\s*模式|VUE项目模式)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 用户提示词中明确指定多文件模式的关键词正则
     */
    private static final Pattern MULTI_FILE_PATTERN = Pattern.compile(
            "(?i)(多文件|三文件|分离CSS|分离JS|HTML\\+CSS\\+JS|样式分离|脚本分离|多文件结构)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 用户提示词中明确指定单HTML模式的关键词正则
     */
    private static final Pattern HTML_PATTERN = Pattern.compile(
            "(?i)(单页模式|单文件模式|单HTML|一个HTML|单文件|all-in-one|内联样式|单HTML页面模式)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 从用户原始提示词中检测是否明确指定了代码生成类型
     *
     * @param originalPrompt 用户原始提示词
     * @return 检测到的类型，未检测到返回 null
     */
    private static CodeGeneratorTypeEnum detectExplicitTypeFromPrompt(String originalPrompt) {
        if (originalPrompt == null || originalPrompt.isBlank()) {
            return null;
        }
        if (VUE_PROJECT_PATTERN.matcher(originalPrompt).find()) {
            return CodeGeneratorTypeEnum.VUE_PROJECT;
        }
        if (MULTI_FILE_PATTERN.matcher(originalPrompt).find()) {
            return CodeGeneratorTypeEnum.MULTI_FILE;
        }
        if (HTML_PATTERN.matcher(originalPrompt).find()) {
            return CodeGeneratorTypeEnum.HTML;
        }
        return null;
    }

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            CodeGeneratorTypeEnum generationType = context.getGenerationType();
            log.info("\n 正在执行节点: 代码生成类型策略节点。\n");
            ThrowUtils.throwIf(generationType == null, ErrorCode.PARAMS_ERROR, "代码生成类型不能为空");
            // 设置代码生成类型
            CodeGeneratorTypeEnum explicitType = detectExplicitTypeFromPrompt(context.getOriginalPrompt());
            CodeGeneratorTypeEnum finalType = generationType;
            try {
                if (CodeGeneratorTypeEnum.AI_STRATEGY.equals(generationType) && explicitType != null) {
                    finalType = explicitType;
                    log.info("检测到用户明确指定代码生成方案，跳过策略模型: {}", finalType.getValue());
                    context.updateAppCodeGenType(finalType);
                } else if (CodeGeneratorTypeEnum.AI_STRATEGY.equals(generationType)) {
                    AiCodeGeneratorTypeStrategyService strategyService =
                            SpringContextUtil.getBean(AiCodeGeneratorTypeStrategyService.class);
                    String promptForStrategy = context.getEnhancedPrompt() != null
                            ? context.getEnhancedPrompt()
                            : context.getOriginalPrompt();
                    finalType = strategyService.getCodeGenStrategy(promptForStrategy);
                    if (finalType == null) {
                        throw new IllegalStateException("代码生成策略模型返回空类型");
                    }
                    log.info("AI 智能选择代码生成方案: {} ({})", finalType.getValue(), finalType.getText());
                    context.updateAppCodeGenType(finalType);
                } else if (explicitType != null && !explicitType.equals(generationType)) {
                    // 用户提示词明确指定类型时，覆盖创建产物时的初始类型
                    log.info("检测到用户提示词中明确指定了 {} 模式，覆盖原配置的 {} 模式",
                            explicitType.getValue(), generationType.getValue());
                    finalType = explicitType;
                    context.updateAppCodeGenType(finalType);
                } else {
                    log.info("直接使用用户指定的代码生成方案: {}", generationType.getValue());
                }
            } catch (Exception e) {
                // 策略模型失败不能无条件降级为 HTML，优先保留用户明确类型，否则使用多文件模式
                finalType = explicitType != null ? explicitType : CodeGeneratorTypeEnum.MULTI_FILE;
                log.error("AI 智能路由失败，降级为 {} 类型: {}", finalType.getValue(), e.getMessage(), e);
                context.updateAppCodeGenType(finalType);
            }


            context.setCurrentStep("代码生成类型策略已完成");
            context.setGenerationType(finalType);
            log.info("\n 代码生成类型策略节点运行完成。\n");
            return WorkflowContext.saveContext(context);
        });
    }
}
