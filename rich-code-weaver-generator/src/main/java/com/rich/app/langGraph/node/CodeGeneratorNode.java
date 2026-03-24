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
 * @create 2026/1/11
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
     * @param appId   产物ID
     * @param emitter 接收每个流式文本块的消费者
     */
    public static void registerStreamEmitter(Long appId, Consumer<String> emitter) {
        // 将流式输出发射器注册到映射表中，用于实时转发代码生成流到前端
        STREAM_EMITTERS.put(appId, emitter);
    }

    /**
     * 注销流式输出发射器（由 CodeGenWorkflowApp 在工作流结束后调用）
     *
     * @param appId 产物ID
     */
    public static void unregisterStreamEmitter(Long appId) {
        // 从映射表中移除流式输出发射器，释放资源
        STREAM_EMITTERS.remove(appId);
    }

    /**
     * 异步创建代码生成节点
     *
     * @return 代码生成节点
     */
    public static AsyncNodeAction<MessagesState<String>> create() {
        // 创建并返回一个异步节点，用于执行 AI 代码生成任务
        return node_async(state -> {
            // 从状态中获取工作流上下文
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("\n 正在执行节点: AI 代码生成。\n");

            // 获取代码生成类型（HTML、MULTI_FILE、VUE_PROJECT 等）
            CodeGeneratorTypeEnum generationType = context.getGenerationType();
            // 获取增强后的提示词（由 PromptEnhancerNode 优化后的提示词）
            String enhancedPrompt = context.getEnhancedPrompt();
            Long appId = context.getAppId();
            
            // 从 Spring 容器中获取代码生成工具类
            AIGenerateCodeAndSaveToFileUtils aIGenerateCodeAndSaveToFileUtils = 
                    SpringContextUtil.getBean(AIGenerateCodeAndSaveToFileUtils.class);

            // 执行 AI 代码生成逻辑，返回流式代码生成结果
            Flux<String> codeStream = aIGenerateCodeAndSaveToFileUtils
                    .aiGenerateAndSaveCodeStream(enhancedPrompt, generationType, appId);
            
            // 获取注册的流式输出发射器，将代码生成流实时转发到前端
            Consumer<String> emitter = STREAM_EMITTERS.get(appId);
            if (emitter != null) {
                // 工作流模式：将每个流式文本块转发到前端，实现与普通流式输出相同的实时展示效果
                // 使用 doOnNext 在每个元素发出时执行发射器，blockLast 等待流完成（最多10分钟）
                codeStream.doOnNext(emitter).blockLast(Duration.ofMinutes(10));
            } else {
                // 无发射器：静默等待流完成（非工作流模式或测试场景）
                codeStream.blockLast(Duration.ofMinutes(10));
            }
            
            // 根据代码生成类型和产物ID构建生成目录路径
            String generatedCodeDir = String.format("%s/%s_%s", 
                    AppConstant.CODE_OUTPUT_ROOT_DIR, generationType.getValue(), appId);
            log.info("AI 代码生成完成，生成目录: {}", generatedCodeDir);

            // 更新工作流上下文状态
            context.setOutputDir(generatedCodeDir);  // 保存生成目录路径
            context.setCurrentStep("AI 代码生成已完成");  // 更新当前步骤
            log.info("\n AI 代码生成节点运行完成。\n");
            
            // 保存更新后的上下文并返回
            return WorkflowContext.saveContext(context);
        });
    }
}
