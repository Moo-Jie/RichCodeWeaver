package com.rich.codeweaver.agent;

import com.rich.codeweaver.service.generator.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Agent 代码生成执行器（入口门面类）
 * 委托给 CodeGenAgent（BaseAgent → ReActAgent → ToolCallAgent → CodeGenAgent）执行
 *
 * @author DuRuiChi
 * @create 2026/4/1
 **/
@Slf4j
@Component
public class CodeGenAgentApp {

    @Resource
    private CodeGenAgent codeGenAgent;

    /**
     * 执行 Agent 代码生成并返回 SSE 流
     * 内部调用链：CodeGenAgent → BaseAgent.runStream() → ReActAgent.step()
     *              → ToolCallAgent.think() + act()
     *
     * @param userMessage        用户消息（需求描述）
     * @param appId              产物 ID
     * @param chatHistoryService 对话历史服务
     * @param userId             用户 ID
     * @return Flux<ServerSentEvent<String>> SSE 响应流
     **/
    public Flux<ServerSentEvent<String>> executeAgent(
            String userMessage,
            Long appId,
            ChatHistoryService chatHistoryService,
            Long userId) {
        return codeGenAgent.executeAgent(userMessage, appId, chatHistoryService, userId);
    }
}
