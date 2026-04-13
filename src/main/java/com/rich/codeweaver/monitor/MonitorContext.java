package com.rich.codeweaver.monitor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 模型监控上下文
 * 在 AI 模型调用的整个生命周期中，通过 ThreadLocal 在线程间传递，作为拼接指标统计的 Key 、创建计数器和计时器的参数。
 *
 * @author DuRuiChi
 * @create 2026/03/31
 * @see MonitorContextHolder
 * @see AiModelMonitorListener
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorContext implements Serializable {

    /**
     * 用户ID
     * <p>
     * 用于统计不同用户的 AI 模型使用情况，支持按用户维度分析 Token 消耗、请求频率等指标。
     * 注意：使用 String 类型而非 Long，因为 Prometheus 标签只支持字符串类型。
     * </p>
     */
    private String userId;

    /**
     * 产物ID
     * <p>
     * 用于统计不同产物的 AI 模型使用情况，支持按产物维度分析代码生成效率、错误率等指标。
     * 注意：使用 String 类型而非 Long，因为 Prometheus 标签只支持字符串类型。
     * </p>
     */
    private String appId;

    /**
     * 代码生成模式
     * <p>
     * 标识当前使用的代码生成模式，支持以下两种模式：
     * <ul>
     *   <li>workflow: 工作流分布执行模式，通过预定义的节点流程生成代码</li>
     *   <li>agent: Agent 自主规划模式，由 AI 自主决策执行步骤</li>
     * </ul>
     * 该字段用于对比分析两种模式的性能差异、Token 消耗、成功率等指标。
     * </p>
     */
    private String genMode;

    /**
     * 序列化版本号
     * <p>
     * 用于控制序列化兼容性，当类结构发生变化时需要更新此版本号。
     * </p>
     */
    @Serial
    private static final long serialVersionUID = 1L;
}
