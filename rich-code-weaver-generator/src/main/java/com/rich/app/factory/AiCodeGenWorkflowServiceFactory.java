package com.rich.app.factory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rich.ai.aiTools.ToolsManager;
import com.rich.ai.rag.RagContentRetrieverAugmentorFactory;
import com.rich.ai.service.AiCodeGeneratorService;
import com.rich.app.service.ChatHistoryService;
import com.rich.client.innerService.InnerSystemPromptService;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.common.utils.SpringContextUtil;
import com.rich.model.enums.CodeGeneratorTypeEnum;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI 服务实例创建工厂，用于通过 AppId 创建 AI 服务实例
 * （在 Caffeine 缓存中创建实例、历史消息存入 Redis）
 *
 * @author DuRuiChi
 * @create 2025/12/5
 **/
@Slf4j
@Configuration
public class AiCodeGenWorkflowServiceFactory {

    /**
     * 创建 Caffeine 缓存服务，用于存储 AI 服务实例
     *
     * @param appId 产物 id
     * @return com.rich.app.service.aiChatService.AiCodeGeneratorService
     * @author DuRuiChi
     * @create 2025/12/18
     **/
    private final Cache<Long, AiCodeGeneratorService> caffeineService = Caffeine.newBuilder()
            // 防止内存泄露的配置
            // 最大缓存 1000 个实例
            .maximumSize(1000)
            // 写入后 30 分钟过期
            .expireAfterWrite(Duration.ofMinutes(30))
            // 访问后 10 分钟过期
            .expireAfterAccess(Duration.ofMinutes(10))
            // 移除时记录日志
            .removalListener((key, value, cause) -> {
                log.debug("【AI 服务实例缓存】已移除，appId: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 基础模型（使用 langchain4j 自带的 OpenAiChatModel）
     * 使用 langchain4j 自动配置的 openAiChatModel
     **/
    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

    /**
     * 供 AI 服务调用的工具包
     **/
    @Resource
    private ToolsManager toolsManager;

    /**
     * Redis 类型的 ChatMemory 存储，用于存储对话历史
     **/
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    /**
     * 对话历史服务，用于从数据库加载和保存对话历史
     **/
    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * 系统提示词内部服务（通过 Dubbo 远程调用 prompt 模块）
     **/
    @DubboReference
    private InnerSystemPromptService innerSystemPromptService;

    /**
     * RAG 知识库检索增强工厂
     * 当 rag.enabled=true 时由 Spring 自动注入，否则为 null
     * 为 null 时 AI 服务正常工作，只是不会从知识库检索参考内容
     **/
    @Autowired
    private RagContentRetrieverAugmentorFactory ragContentRetrieverAugmentorFactory;

    /**
     * 在 Caffeine 缓存中获取或创建 AI 服务实例
     * 通过 appId 从缓存中获取已存在的实例，若不存在则创建新实例并加入缓存
     *
     * @param appId           产物ID
     * @param codeGenTypeEnum 代码生成类型枚举
     * @return AiCodeGeneratorService AI 代码生成服务实例
     * @author DuRuiChi
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(Long appId, CodeGeneratorTypeEnum codeGenTypeEnum) {
        // 从 Caffeine 缓存中获取或创建 AI 服务实例
        // 缓存策略：相同 appId 会复用已构建的实例，避免重复从数据库加载历史对话记录
        // 缓存未命中时，会自动调用 createAiCodeGeneratorService 方法创建新实例
        return caffeineService.get(appId, (key) -> this.createAiCodeGeneratorService(appId, codeGenTypeEnum));
    }

    /**
     * 根据 appId 创建独立的 AI 服务实例，并从数据库加载对话历史
     * 根据代码生成类型选择不同的 AI 模型和配置（普通流式模型或推理流式模型）
     *
     * @param appId           产物ID
     * @param codeGenTypeEnum 代码生成类型枚举
     * @return AiCodeGeneratorService AI 代码生成服务实例
     * @author DuRuiChi
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGeneratorTypeEnum codeGenTypeEnum) {
        log.info("为 appId: {} 创建新的 AI 服务实例，代码生成类型: {}", appId, codeGenTypeEnum);

        // 步骤1：根据代码生成类型从数据库查询对应的系统提示词
        String promptKey = getPromptKeyByCodeGenType(codeGenTypeEnum);
        String systemPromptContent = innerSystemPromptService.getPromptContentByKey(promptKey);
        if (systemPromptContent == null || systemPromptContent.isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "未找到系统提示词，promptKey=" + promptKey + "，请在管理后台配置");
        }

        // 步骤2：根据 appId 创建独立的对话记忆（ChatMemory）
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)  // 使用 appId 作为唯一标识
                .chatMemoryStore(redisChatMemoryStore)  // 指定为 Redis 类型的存储
                .maxMessages(50)  // 最大保留50条消息（滑动窗口）
                .build();

        // 步骤3：从数据库中加载对话历史到 Redis 类型的 chatMemory 中
        Boolean isSave = chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 10);
        if (!isSave) {
            // 若加载失败，添加提示消息并记录日志
            chatMemory.add(UserMessage.from("历史记录加载失败，可能已经过期。"));
            log.warn("为 appId: {} 加载历史记录失败", appId);
        }

        // 步骤4：构建 AI 服务实例的基础配置（使用 systemMessageProvider 编程式指定系统提示词）
        AiServices<AiCodeGeneratorService> aiCodeGenServices = AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)  // 配置基础 AI 模型
                .systemMessageProvider(memoryId -> systemPromptContent)  // 从数据库查询的系统提示词
                // TODO 配置提示词护轨规则，目前规则不够完善，容易导致误判
//                .inputGuardrails(new PromptSafetyInputGuardrail())
                .maxSequentialToolsInvocations(25)  // 最大连续调用工具次数（防止无限循环）
                .chatMemory(chatMemory);  // 配置对话记忆

        // 步骤4.5：注入 RAG 知识库检索增强器（如果 RAG 功能已启用）
        // RetrievalAugmentor 会在每次 AI 调用前自动执行：
        //   1. 将用户查询向量化
        //   2. 在 PGVector 中按 codeGenType 过滤检索相关知识库片段
        //   3. 将检索到的开发规范以权威参考形式注入到用户消息中
        // 这样 AI 生成代码时会参考知识库中的开发规范，降低幻觉概率
        if (ragContentRetrieverAugmentorFactory != null) {
            RetrievalAugmentor ragAugmentor = ragContentRetrieverAugmentorFactory
                    .createWorkflowRetrievalAugmentor(codeGenTypeEnum.name());
            aiCodeGenServices.retrievalAugmentor(ragAugmentor);
            log.info("为 appId: {} 注入 RAG 知识库检索增强，codeGenType: {}", appId, codeGenTypeEnum.name());
        } else {
            log.debug("RAG 知识库未启用，appId: {} 跳过检索增强", appId);
        }

        // 步骤5：根据代码生成类型选择不同的 AI 模型和配置
        switch (codeGenTypeEnum) {
            // 单文件模式、多文件模式：使用普通流式模型
            case HTML, MULTI_FILE -> {
                // 从 Spring 容器中获取普通流式 AI 模型（多例模式）
                StreamingChatModel streamingChatModel = SpringContextUtil.getBean("streamingChatModel", StreamingChatModel.class);
                return aiCodeGenServices
                        .streamingChatModel(streamingChatModel)  // 配置流式模型
                        .build();
            }
            // Vue 项目工程模式：使用推理流式模型（支持工具调用）
            case VUE_PROJECT -> {
                // 从 Spring 容器中获取推理流式 AI 模型（多例模式）
                StreamingChatModel reasoningStreamingChatModel = SpringContextUtil.getBean("reasoningStreamingChatModel", StreamingChatModel.class);
                return aiCodeGenServices
                        .streamingChatModel(reasoningStreamingChatModel)  // 配置推理流式模型
                        .chatMemoryProvider(id -> chatMemory)  // 强制指定 chatMemoryProvider（推理模式要求）
                        .tools(toolsManager.getAllTools())  // 指定供 AI 调用的自定义工具包
                        // 当 AI 幻觉调用不存在的工具时，返回错误提示（防止异常）
                        // 参考：https://blog.csdn.net/qq_52155674/article/details/147238250
                        .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                                toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()))
                        .build();
            }
            // 默认模式：使用基础配置
            default -> {
                log.warn("未知的代码生成类型: {}，使用默认配置", codeGenTypeEnum);
                return aiCodeGenServices.build();
            }
        }
    }

    /**
     * 根据代码生成类型获取对应的系统提示词标识
     *
     * @param codeGenTypeEnum 代码生成类型枚举
     * @return 系统提示词标识（promptKey）
     */
    private String getPromptKeyByCodeGenType(CodeGeneratorTypeEnum codeGenTypeEnum) {
        return switch (codeGenTypeEnum) {
            case HTML -> "html-system-prompt";
            case MULTI_FILE -> "multi-file-system-prompt";
            case VUE_PROJECT -> "vue-project-system-prompt";
            default -> "html-system-prompt";
        };
    }
}
