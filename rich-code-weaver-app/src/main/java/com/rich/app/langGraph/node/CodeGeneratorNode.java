package com.rich.app.langGraph.node;

import com.rich.app.langGraph.state.WorkflowContext;
import com.rich.app.utils.AIGenerateCodeAndSaveToFileUtils;
import com.rich.common.constant.AppConstant;
import com.rich.common.utils.SpringContextUtil;
import com.rich.model.enums.CodeGeneratorTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

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
     * 流式输出发射器注册表，用于在工作流模式下将代码生成流实时转发到前端
     * key: appId, value: 接收流式文本块的消费者
     */
    private static final ConcurrentHashMap<Long, Consumer<String>> STREAM_EMITTERS = new ConcurrentHashMap<>();

    /**
     * 注册流式输出发射器（由 CodeGenWorkflowApp 在工作流启动前调用）
     *
     * @param appId   应用ID
     * @param emitter 接收每个流式文本块的消费者
     */
    public static void registerStreamEmitter(Long appId, Consumer<String> emitter) {
        STREAM_EMITTERS.put(appId, emitter);
    }

    /**
     * 注销流式输出发射器（由 CodeGenWorkflowApp 在工作流结束后调用）
     *
     * @param appId 应用ID
     */
    public static void unregisterStreamEmitter(Long appId) {
        STREAM_EMITTERS.remove(appId);
    }

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

            // 执行代码生成逻辑
            Flux<String> codeStream = aIGenerateCodeAndSaveToFileUtils.aiGenerateAndSaveCodeStream(enhancedPrompt, generationType, appId);
            // 获取注册的流式输出发射器，将代码生成流实时转发到前端
            Consumer<String> emitter = STREAM_EMITTERS.get(appId);
            if (emitter != null) {
                // 工作流模式：将每个流式文本块转发到前端，实现与普通流式输出相同的实时展示效果
                codeStream.doOnNext(emitter).blockLast(Duration.ofMinutes(10));
            } else {
                // 无发射器：静默等待流完成
                codeStream.blockLast(Duration.ofMinutes(10));
            }
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
