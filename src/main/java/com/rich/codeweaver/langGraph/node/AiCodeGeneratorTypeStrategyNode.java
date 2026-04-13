package com.rich.codeweaver.langGraph.node;

import com.rich.codeweaver.common.exception.ErrorCode;
import com.rich.codeweaver.common.exception.ThrowUtils;
import com.rich.codeweaver.langGraph.state.WorkflowContext;
import com.rich.codeweaver.model.enums.CodeGeneratorTypeEnum;
import com.rich.codeweaver.service.generator.AiCodeGeneratorTypeStrategyService;
import com.rich.codeweaver.common.utils.SpringContextUtil;
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
            CodeGeneratorTypeEnum finalType = generationType;
            try {
                if (CodeGeneratorTypeEnum.AI_STRATEGY.equals(generationType)) {
                    // 获取 AI 代码生成类型策略服务
                    AiCodeGeneratorTypeStrategyService aiCodeGeneratorTypeStrategyService = SpringContextUtil.getBean(AiCodeGeneratorTypeStrategyService.class);
                    // 让 AI 根据增强后的提示词选择代码生成方案（增强后包含网络资源、图片等）
                    String promptForStrategy = context.getEnhancedPrompt() != null ? context.getEnhancedPrompt() : context.getOriginalPrompt();
                    finalType = aiCodeGeneratorTypeStrategyService.getCodeGenStrategy(promptForStrategy);
                    log.info("AI 智能选择代码生成方案: {} ({})", finalType.getValue(), finalType.getText());

                    // 更新数据库中的 codeGenType 为 AI 判断后的实际类型
                    context.updateAppCodeGenType(finalType);
                } else {
                    // 即使用户在创建产物时选择了固定类型，也要检查提示词中是否明确指定了不同的类型
                    // 例如：产物创建时选了 single_html，但提示词写了 "VUE项目模式"
                    CodeGeneratorTypeEnum promptExplicitType = detectExplicitTypeFromPrompt(context.getOriginalPrompt());
                    if (promptExplicitType != null && !promptExplicitType.equals(generationType)) {
                        log.info("检测到用户提示词中明确指定了 {} 模式，覆盖原配置的 {} 模式",
                                promptExplicitType.getValue(), generationType.getValue());
                        finalType = promptExplicitType;
                        // 更新数据库中的 codeGenType
                        context.updateAppCodeGenType(finalType);
                    } else {
                        log.info("直接使用用户指定的代码生成方案: {}", generationType.getValue());
                    }
                }
            } catch (Exception e) {
                log.error("AI 智能路由失败，使用默认 HTML 类型: {}", e.getMessage());
                finalType = CodeGeneratorTypeEnum.HTML;
                // 更新数据库中的 codeGenType 为默认类型
                context.updateAppCodeGenType(finalType);
            }

            // 状态更新
            context.setCurrentStep("代码生成类型策略已完成");
            context.setGenerationType(finalType);
            log.info("\n 代码生成类型策略节点运行完成。\n");
            return WorkflowContext.saveContext(context);
        });
    }
}
