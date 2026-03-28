package com.rich.ai.agent;

import lombok.Getter;

/**
 * Agent 执行状态枚举
 *
 * @author DuRuiChi
 * @create 2026/4/1
 **/
@Getter
public enum AgentState {

    IDLE("空闲"),
    RUNNING("执行中"),
    FINISHED("已完成"),
    ERROR("异常");

    private final String text;

    AgentState(String text) {
        this.text = text;
    }
}
