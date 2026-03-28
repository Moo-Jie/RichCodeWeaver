package com.rich.app.agent;

import cn.hutool.core.util.StrUtil;
import com.rich.ai.agent.AiCodeGenAgentService;
import com.rich.app.factory.AiCodeGenAgentServiceFactory;
import com.rich.app.service.ChatHistoryService;
import com.rich.app.utils.ConvertTokenStreamToFluxUtils.ConvertAgentTokenStreamToFluxUtils;
import com.rich.app.utils.streamHandle.JsonStreamHandler;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 代码生成 Agent（简化版）
 * 直接调用 Langchain4j AiServices，框架内部托管完整的 ReAct 工具调用循环
 *
 * 调用链：
 * CodeGenAgent.executeAgent()
 *   → AiCodeGenAgentService.chatStream()  [Langchain4j 内部处理 ReAct 循环]
 *   → ConvertAgentTokenStreamToFluxUtils  [TokenStream → Flux<String>]
 *   → JsonStreamHandler                   [Flux<String> → SSE 事件]
 *
 * 说明：
 * - Langchain4j AiServices 内部完整处理 ReAct 循环（Think → Act → Observe → Think...）
 * - maxSequentialToolsInvocations=40 控制最大工具调用次数
 * - 无需手动管理 Agent 状态、步骤计数等，框架已托管
 *
 * @author DuRuiChi
 * @create 2026/4/1
 **/
@Slf4j
@Component
public class CodeGenAgent {

    @Resource
    private AiCodeGenAgentServiceFactory agentServiceFactory;

    @Resource
    private ConvertAgentTokenStreamToFluxUtils convertAgentTokenStreamToFluxUtils;

    @Resource
    private JsonStreamHandler jsonStreamHandler;

    /**
     * 执行 Agent 代码生成并返回 SSE 流
     *
     * @param userMessage        用户消息（需求描述）
     * @param appId              产物 ID
     * @param chatHistoryService 对话历史服务
     * @param userId             用户 ID
     * @return Flux<ServerSentEvent<String>> SSE 响应流
     */
    public Flux<ServerSentEvent<String>> executeAgent(
            String userMessage,
            Long appId,
            ChatHistoryService chatHistoryService,
            Long userId) {

        log.info("[CodeGenAgent] 启动代码生成，appId={}, userId={}", appId, userId);

        // 1. 获取 Agent 服务实例（含系统提示词、工具、对话记忆）
        AiCodeGenAgentService agentService = agentServiceFactory.getAgentService(appId);

        // 2. 调用 Langchain4j AiServices（框架内部托管完整 ReAct 循环）
        TokenStream tokenStream = agentService.chatStream(userMessage, appId);

        // 3. 将 TokenStream 转换为 Flux<String>
        Flux<String> agentFlux = convertAgentTokenStreamToFluxUtils.convertTokenStreamToFlux(tokenStream, appId);

        // 4. 通过 JsonStreamHandler 解析 JSON 消息块，封装为 SSE 事件（同时保存对话历史）
        return jsonStreamHandler.handleStream(
                agentFlux.filter(StrUtil::isNotEmpty),
                chatHistoryService,
                appId,
                userId
        );
    }
}
