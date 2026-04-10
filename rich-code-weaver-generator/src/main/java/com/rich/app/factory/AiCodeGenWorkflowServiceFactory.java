package com.rich.app.factory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rich.ai.aiTools.ToolsManager;
import com.rich.ai.rag.RagContentRetrieverAugmentorFactory;
import com.rich.ai.service.AiCodeGeneratorService;
import com.rich.app.service.ChatHistoryService;
import com.rich.client.innerService.InnerSystemPromptService;
import com.rich.common.constant.AiServiceConstant;
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
import dev.langchain4j.service.tool.ToolProvider;
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
     * RAG 未启用日志模板
     */
    private static final String RAG_DISABLED_LOG_TEMPLATE = "RAG 知识库未启用，appId: {} 跳过检索增强";

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
            .maximumSize(AiServiceConstant.AI_SERVICE_CACHE_MAX_SIZE)
            // 写入后 30 分钟过期
            .expireAfterWrite(Duration.ofMinutes(AiServiceConstant.AI_SERVICE_EXPIRE_AFTER_WRITE_MINUTES))
            // 访问后 10 分钟过期
            .expireAfterAccess(Duration.ofMinutes(AiServiceConstant.AI_SERVICE_EXPIRE_AFTER_ACCESS_MINUTES))
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
     * MCP 工具提供器（可选注入）
     * 仅在启用 MCP 且成功完成配置装配时存在
     **/
    @Autowired(required = false)
    private ToolProvider mcpToolProvider;

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
        return caffeineService.get(appId, key -> this.createAiCodeGeneratorService(appId, codeGenTypeEnum));
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

        // 1：根据代码生成类型从数据库查询对应的系统提示词
        String systemPromptContent = loadSystemPromptContent(codeGenTypeEnum);

        // 2：根据 appId 创建独立的对话记忆（ChatMemory）
        MessageWindowChatMemory chatMemory = buildChatMemory(appId);

        // 3：从数据库中加载对话历史到 Redis 类型的 chatMemory 中
        loadWorkflowChatHistory(appId, chatMemory);

        // 4：构建 AI 服务实例的基础配置（使用 systemMessageProvider 编程式指定系统提示词）
        AiServices<AiCodeGeneratorService> aiCodeGenServices = buildBaseWorkflowAiService(systemPromptContent, chatMemory);

        // 4.5：注入 RAG 知识库检索增强器（如果 RAG 功能已启用）
        // RetrievalAugmentor 会在每次 AI 调用前自动执行：
        //   1. 将用户查询向量化
        //   2. 在 PGVector 中按 codeGenType 过滤检索相关知识库片段
        //   3. 将检索到的开发规范以权威参考形式注入到用户消息中
        // 这样 AI 生成代码时会参考知识库中的开发规范，降低幻觉概率
        attachWorkflowRagAugmentor(aiCodeGenServices, appId, codeGenTypeEnum);

        // 5：根据代码生成类型选择不同的 AI 模型和配置
        switch (codeGenTypeEnum) {
            // 单文件模式、多文件模式：使用普通流式模型
            case HTML, MULTI_FILE -> {
                return buildStreamingWorkflowService(aiCodeGenServices);
            }
            // Vue 项目工程模式：使用推理流式模型（支持工具调用）
            case VUE_PROJECT -> {
                return buildReasoningWorkflowService(aiCodeGenServices, chatMemory);
            }
            // 默认模式：使用基础配置
            default -> {
                log.warn("未知的代码生成类型: {}，使用默认配置", codeGenTypeEnum);
                return aiCodeGenServices.build();
            }
        }
    }

    /**
     * 构建工作流模式的对话记忆
     *
     * @param appId 产物 ID
     * @return 对话记忆
     */
    private MessageWindowChatMemory buildChatMemory(long appId) {
        return MessageWindowChatMemory
                .builder()
                .id(appId)  // 使用 appId 作为唯一标识
                .chatMemoryStore(redisChatMemoryStore)  // 指定为 Redis 类型的存储
                .maxMessages(AiServiceConstant.CHAT_MEMORY_MAX_MESSAGES)  // 最大保留固定条数消息（滑动窗口）
                .build();
    }

    /**
     * 加载工作流模式系统提示词
     *
     * @param codeGenTypeEnum 代码生成类型
     * @return 系统提示词内容
     */
    private String loadSystemPromptContent(CodeGeneratorTypeEnum codeGenTypeEnum) {
        String promptKey = getPromptKeyByCodeGenType(codeGenTypeEnum);
        String systemPromptContent = innerSystemPromptService.getPromptContentByKey(promptKey);
        if (systemPromptContent == null || systemPromptContent.isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "未找到系统提示词，promptKey=" + promptKey + "，请在管理后台配置");
        }
        return systemPromptContent;
    }

    /**
     * 加载工作流模式历史消息到记忆中
     *
     * @param appId 产物 ID
     * @param chatMemory 对话记忆
     */
    private void loadWorkflowChatHistory(long appId, MessageWindowChatMemory chatMemory) {
        Boolean isSave = chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, AiServiceConstant.DEFAULT_HISTORY_LOAD_LIMIT);
        if (!isSave) {
            chatMemory.add(UserMessage.from(AiServiceConstant.HISTORY_LOAD_FAILED_MESSAGE));
            log.warn("为 appId: {} 加载历史记录失败", appId);
        }
    }

    /**
     * 构建工作流模式通用 AI 服务配置
     *
     * @param systemPromptContent 系统提示词
     * @param chatMemory 对话记忆
     * @return AI 服务构建器
     */
    private AiServices<AiCodeGeneratorService> buildBaseWorkflowAiService(String systemPromptContent,
                                                                          MessageWindowChatMemory chatMemory) {
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .systemMessageProvider(memoryId -> systemPromptContent)
                .maxSequentialToolsInvocations(AiServiceConstant.WORKFLOW_MAX_TOOLS_INVOCATIONS)
                .chatMemory(chatMemory);
    }

    /**
     * 为工作流模式挂载 RAG 检索增强器
     *
     * @param aiCodeGenServices AI 服务构建器
     * @param appId 产物 ID
     * @param codeGenTypeEnum 代码生成类型
     */
    private void attachWorkflowRagAugmentor(AiServices<AiCodeGeneratorService> aiCodeGenServices,
                                            long appId, CodeGeneratorTypeEnum codeGenTypeEnum) {
        if (ragContentRetrieverAugmentorFactory == null) {
            log.debug(RAG_DISABLED_LOG_TEMPLATE, appId);
            return;
        }
        RetrievalAugmentor ragAugmentor = ragContentRetrieverAugmentorFactory
                .createWorkflowRetrievalAugmentor(codeGenTypeEnum.name());
        aiCodeGenServices.retrievalAugmentor(ragAugmentor);
        log.info("为 appId: {} 注入 RAG 知识库检索增强，codeGenType: {}", appId, codeGenTypeEnum.name());
    }

    /**
     * 构建普通流式工作流服务
     *
     * @param aiCodeGenServices AI 服务构建器
     * @return AI 服务实例
     */
    private AiCodeGeneratorService buildStreamingWorkflowService(AiServices<AiCodeGeneratorService> aiCodeGenServices) {
        StreamingChatModel streamingChatModel = SpringContextUtil.getBean(
                AiServiceConstant.STREAMING_CHAT_MODEL_BEAN, StreamingChatModel.class);
        return aiCodeGenServices
                .streamingChatModel(streamingChatModel)
                .build();
    }

    /**
     * 构建推理模式工作流服务
     *
     * @param aiCodeGenServices AI 服务构建器
     * @param chatMemory 对话记忆
     * @return AI 服务实例
     */
    private AiCodeGeneratorService buildReasoningWorkflowService(AiServices<AiCodeGeneratorService> aiCodeGenServices,
                                                                 MessageWindowChatMemory chatMemory) {
        // 推理模式单独使用 reasoning-streaming-chat-model，普通流式模式不会走这里
        StreamingChatModel reasoningStreamingChatModel = SpringContextUtil.getBean(
                AiServiceConstant.REASONING_STREAMING_CHAT_MODEL_BEAN, StreamingChatModel.class);

        // 推理模式支持工具调用，因此在原有本地工具之外，额外挂载 MCP ToolProvider
        AiServices<AiCodeGeneratorService> builder = aiCodeGenServices
                // 指定推理流式模型，保证具备更强的工具调用能力
                .streamingChatModel(reasoningStreamingChatModel)
                // 继续沿用当前 appId 对应的 chatMemory，保证本轮推理与历史上下文连贯
                .chatMemoryProvider(id -> chatMemory)
                // 本地工具依旧保留，不会因为引入 MCP 而替换掉原有工具链
                .tools(toolsManager.getAllTools())
                // 当模型幻觉出不存在的工具名时，返回统一错误结果，避免调用链直接中断
                .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                        toolExecutionRequest,
                        String.format(AiServiceConstant.HALLUCINATED_TOOL_MESSAGE_TEMPLATE, toolExecutionRequest.name())));
        if (mcpToolProvider != null) {
            // 只有启用了 MCP 时才注入，未启用时保持原有行为不变
            // 这里是“追加”一个外部工具提供器，而不是替换前面的本地工具
            builder.toolProvider(mcpToolProvider);
            log.info("为工作流推理服务注入 MCP 工具提供器");
        }

        // 最终构建出可直接执行推理 + 工具调用的 AI 服务实例
        return builder.build();
    }

    /**
     * 根据代码生成类型获取对应的系统提示词标识
     *
     * @param codeGenTypeEnum 代码生成类型枚举
     * @return 系统提示词标识（promptKey）
     */
    private String getPromptKeyByCodeGenType(CodeGeneratorTypeEnum codeGenTypeEnum) {
        return switch (codeGenTypeEnum) {
            case HTML -> AiServiceConstant.HTML_SYSTEM_PROMPT_KEY;
            case MULTI_FILE -> AiServiceConstant.MULTI_FILE_SYSTEM_PROMPT_KEY;
            case VUE_PROJECT -> AiServiceConstant.VUE_PROJECT_SYSTEM_PROMPT_KEY;
            default -> AiServiceConstant.HTML_SYSTEM_PROMPT_KEY;
        };
    }
}
