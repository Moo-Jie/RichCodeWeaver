package com.rich.app.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * MCP 配置类
 * 负责读取 MCP 服务端 JSON 配置，构建 Langchain4j 的 MCP Client 和 ToolProvider
 * 文档：https://langchain4j.cn/tutorials/mcp.html
 *
 * @author DuRuiChi
 * @create 2026/4/10
 **/
@Slf4j
@Configuration
@EnableConfigurationProperties(McpProperties.class)
@ConditionalOnProperty(prefix = "mcp", name = "enabled", havingValue = "true")
public class McpConfiguration {

    private static final String NPX_COMMAND = "npx";
    private static final String NPX_WINDOWS_COMMAND = "npx.cmd";

    private final McpProperties mcpProperties;
    private final ResourceLoader resourceLoader;
    private final Environment environment;
    private final ObjectMapper objectMapper;
    private final List<McpClient> managedClients = new ArrayList<>();

    /**
     * 构造方法
     *
     * @param mcpProperties MCP 配置属性
     * @param resourceLoader Spring 资源加载器
     * @param environment Spring 环境变量解析器
     * @param objectMapper Jackson 对象映射器
     */
    public McpConfiguration(McpProperties mcpProperties,
                            ResourceLoader resourceLoader,
                            Environment environment,
                            ObjectMapper objectMapper) {
        this.mcpProperties = mcpProperties;
        this.resourceLoader = resourceLoader;
        this.environment = environment;
        this.objectMapper = objectMapper;
    }

    /**
     * 构建 MCP ToolProvider
     * 若当前未配置任何有效的 MCP 服务端，则返回一个空实现，避免影响现有业务流程
     *
     * @return ToolProvider MCP 工具提供器
     */
    @Bean
    public ToolProvider mcpToolProvider() {
        // 先根据 JSON 配置构建所有 MCP Client
        List<McpClient> mcpClients = buildMcpClients();
        if (mcpClients.isEmpty()) {
            // 没有可用服务端时返回空 ToolProvider
            log.warn("MCP 已启用，但未加载到任何可用服务端配置: {}", mcpProperties.getConfigPath());
            return request -> null;
        }

        // 保存到成员变量中，方便在 Spring 容器销毁时统一关闭底层 stdio 进程
        managedClients.clear();
        managedClients.addAll(mcpClients);

        // 构建 Langchain4j 的 McpToolProvider，作为 AiServices 的构建参数注入
        log.info("MCP 工具提供器初始化完成，serverCount={}", mcpClients.size());
        return McpToolProvider.builder()
                .mcpClients(mcpClients)
                .failIfOneServerFails(mcpProperties.isFailIfOneServerFails())
                .build();
    }

    /**
     * Spring 容器销毁时关闭 MCP Client
     * 避免 stdio 子进程泄露
     */
    @PreDestroy
    public void destroy() {
        // 逐个关闭已创建的 MCP Client，避免应用关闭后子进程残留
        for (McpClient client : managedClients) {
            try {
                // 当前版本的 DefaultMcpClient 支持 close，这里做一层防御性判断
                if (client instanceof AutoCloseable autoCloseable) {
                    // 若可以关闭则关闭
                    autoCloseable.close();
                }
            } catch (Exception e) {
                log.warn("关闭 MCP Client 失败", e);
            }
        }

        // 清空引用，避免销毁阶段重复关闭
        managedClients.clear();
    }

    /**
     * 根据 JSON 配置批量构建 MCP Client
     *
     * @return MCP Client 列表
     */
    private List<McpClient> buildMcpClients() {
        // 1. 读取并加载 mcp.config.json 配置文件
        McpServersFileConfig fileConfig = loadFileConfig();
        // 判空：配置文件不存在 / 没有配置任何 MCP 服务，直接返回空列表
        if (fileConfig == null || fileConfig.getMcpServers() == null || fileConfig.getMcpServers().isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 用于存储所有成功创建的 MCP 客户端的列表
        List<McpClient> clients = new ArrayList<>();

        // 3. 遍历配置文件中 所有的 MCP 服务
        for (Map.Entry<String, McpServerConfig> entry : fileConfig.getMcpServers().entrySet()) {
            // 服务唯一标识（比如：baidu-map）
            String serverKey = entry.getKey();
            // 服务配置（command/args/env）
            McpServerConfig serverConfig = entry.getValue();

            // 4. 校验 command 是启动子进程的必需参数，缺失直接跳过
            if (serverConfig == null || serverConfig.getCommand() == null || serverConfig.getCommand().isBlank()) {
                log.warn("跳过无效的 MCP 服务配置，serverKey={}", serverKey);
                continue;
            }

            // 构建传输层（启动子进程）
            // StdioMcpTransport：通过 标准输入输出 与本地 MCP 子进程通信
            McpTransport transport = new StdioMcpTransport.Builder()
                    // 拼接启动命令（如 npx -y @baidumap/mcp-server-baidu-map）
                    .command(buildCommand(serverConfig))
                    // 注入环境变量（如 BAIDU_MAP_API_KEY: xxx）
                    .environment(resolveEnvironment(serverConfig.getEnv()))
                    // 是否打印通信日志（排查问题用）
                    .logEvents(mcpProperties.isLogEvents())
                    .build();

            // 构建 MCP 客户端
            McpClient client = new DefaultMcpClient.Builder()
                    .key(serverKey)    // 绑定唯一标识（baidu-map）
                    .transport(transport) // 绑定子进程通信通道
                    .build();

            // 5. 把创建好的客户端加入列表
            clients.add(client);
            log.info("已加载 MCP 服务端配置，serverKey={}", serverKey);
        }

        // 6. 返回所有 MCP 客户端，供 AI 调用
        return clients;
    }

    /**
     * 读取 MCP 服务端 JSON 配置文件
     *
     * @return MCP 服务端配置
     */
    private McpServersFileConfig loadFileConfig() {
        // 支持从 classpath: 或文件系统路径读取 MCP JSON 配置
        Resource resource = resourceLoader.getResource(mcpProperties.getConfigPath());
        if (!resource.exists()) {
            log.warn("MCP 配置文件不存在: {}", mcpProperties.getConfigPath());
            return null;
        }
        try (InputStream inputStream = resource.getInputStream()) {
            // 反序列化为内部定义的配置对象，结构与 mcp-servers.json 保持一致
            return objectMapper.readValue(inputStream, new TypeReference<McpServersFileConfig>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException("读取 MCP 配置文件失败: " + mcpProperties.getConfigPath(), e);
        }
    }

    /**
     * 构建 MCP stdio 启动命令
     *
     * @param serverConfig 单个服务端配置
     * @return 命令列表
     */
    private List<String> buildCommand(McpServerConfig serverConfig) {
        // command 列表最终会直接交给 ProcessBuilder，用于启动 MCP Server 子进程
        List<String> command = new ArrayList<>();

        // 先放入主命令，例如 npx / uvx / python 等
        command.add(resolveCommand(serverConfig.getCommand()));
        if (serverConfig.getArgs() != null && !serverConfig.getArgs().isEmpty()) {
            // 再顺序拼接命令参数，并对每一项做占位符解析
            command.addAll(serverConfig.getArgs().stream()
                    .filter(Objects::nonNull)
                    .map(this::resolvePlaceholders)
                    .toList());
        }
        return command;
    }

    /**
     * 解析 MCP 服务端环境变量
     *
     * @param sourceEnv 原始环境变量配置
     * @return 解析占位符后的环境变量
     */
    private Map<String, String> resolveEnvironment(Map<String, String> sourceEnv) {
        if (sourceEnv == null || sourceEnv.isEmpty()) {
            // 没有配置环境变量时返回空 Map，避免传 null 给 Builder
            return Collections.emptyMap();
        }

        // 使用 LinkedHashMap 保持配置文件中的顺序，便于调试查看
        Map<String, String> resolvedEnv = new LinkedHashMap<>();
        // 逐个解析占位符，例如 ${baidu.map.api-key}
        sourceEnv.forEach((key, value) -> resolvedEnv.put(key, resolvePlaceholders(value)));
        return resolvedEnv;
    }

    /**
     * 解析启动命令
     * 在 Windows 环境下自动把 npx 替换为 npx.cmd
     *
     * @param command 原始命令
     * @return 兼容当前系统的命令
     */
    private String resolveCommand(String command) {
        // 先解析命令中的占位符，保证支持配置化命令路径
        String resolvedCommand = resolvePlaceholders(command);
        if (isWindows() && NPX_COMMAND.equalsIgnoreCase(resolvedCommand)) {
            // Windows 下 npx 实际需要通过 npx.cmd 启动，否则子进程可能无法正常拉起
            return NPX_WINDOWS_COMMAND;
        }
        return resolvedCommand;
    }

    /**
     * 解析字符串中的 Spring 占位符
     *
     * @param value 原始字符串
     * @return 解析后的字符串
     */
    private String resolvePlaceholders(String value) {
        if (value == null) {
            return null;
        }

        // 这里使用 Spring 环境解析占位符，缺失变量时会直接抛错，避免带着错误配置继续运行
        return environment.resolveRequiredPlaceholders(value);
    }

    /**
     * 判断当前系统是否为 Windows
     *
     * @return true-是 false-否
     */
    private boolean isWindows() {
        // 通过 os.name 做简单判断，满足当前项目对 Windows 开发环境的兼容需求
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /**
     * MCP 服务端配置文件根节点
     **/
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class McpServersFileConfig {
        private Map<String, McpServerConfig> mcpServers;
    }

    /**
     * 单个 MCP 服务端配置
     **/
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class McpServerConfig {

        private String command;

        private List<String> args;

        private Map<String, String> env;
    }
}
