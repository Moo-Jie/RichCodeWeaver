package com.rich.richcodeweaver.langGraph.node;

import com.rich.richcodeweaver.constant.AppConstant;
import com.rich.richcodeweaver.langGraph.state.WorkflowContext;
import com.rich.richcodeweaver.model.enums.CodeGeneratorTypeEnum;
import com.rich.richcodeweaver.utils.SpringContextUtil;
import com.rich.richcodeweaver.utils.aiUtils.AIGenerateCodeAndSaveToFileUtils;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import reactor.core.publisher.Flux;

import java.time.Duration;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 代码生成节点
 *
 * @author DuRuiChi
 * @create 2025/9/11
 **/
@Slf4j
public class CodeGeneratorNode {
    /**
     * 异步创建代码生成节点
     *
     * @return 代码生成节点
     */
    public static AsyncNodeAction<MessagesState<String>> create() {
        // 返回一个异步节点
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("\n 正在执行节点: AI 代码生成。\n");

            CodeGeneratorTypeEnum generationType = context.getGenerationType();
            // 获取增强后的提示词
            String enhancedPrompt = context.getEnhancedPrompt();
            Long appId = context.getAppId();
            AIGenerateCodeAndSaveToFileUtils aIGenerateCodeAndSaveToFileUtils = SpringContextUtil.getBean(AIGenerateCodeAndSaveToFileUtils.class);

            // 执行代码生成逻辑，Agent 模式下忽略代码生成过程的展示（只输出分布执行的步骤）
            Flux<String> codeStream = aIGenerateCodeAndSaveToFileUtils.aiGenerateAndSaveCodeStream(enhancedPrompt, generationType, appId);
            // 等待流式输出完成
            codeStream.blockLast(Duration.ofMinutes(10));
            // 根据类型设置生成目录
            String generatedCodeDir = String.format("%s/%s_%s", AppConstant.CODE_OUTPUT_ROOT_DIR, generationType.getValue(), appId);
            log.info("AI 代码生成完成，生成目录: {}", generatedCodeDir);

            // 更新状态
            context.setOutputDir(generatedCodeDir);
            context.setCurrentStep("AI 代码生成已完成");
            log.info("\n AI 代码生成节点运行完成。\n");
            return WorkflowContext.saveContext(context);
        });
    }
}
