package com.rich.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MCP 配置属性
 * 用于承接 application.yml / application-local.yml 中的 mcp.* 配置
 *
 * @author DuRuiChi
 * @create 2026/4/10
 **/
@Data
@ConfigurationProperties(prefix = "mcp")
public class McpProperties {

    /**
     * MCP 功能总开关
     **/
    private boolean enabled;

    /**
     * MCP 服务端配置文件路径
     * 默认从 classpath 下读取 mcp-servers.json
     **/
    private String configPath = "classpath:mcp-servers.json";

    /**
     * 是否打印 MCP 传输层事件日志
     **/
    private boolean logEvents = true;

    /**
     * 某个 MCP 服务端初始化失败时，是否让整个 ToolProvider 构建失败
     **/
    private boolean failIfOneServerFails = false;
}
