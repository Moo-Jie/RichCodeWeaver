package com.rich.codeweaver.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 产物封装类
 */
@Data
public class AppVO implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;
    /**
     * 产物名称
     */
    private String appName;
    /**
     * 产物封面
     */
    private String cover;
    /**
     * 产物初始化的 prompt
     */
    private String initPrompt;
    /**
     * 代码生成类型（枚举）
     */
    private String codeGenType;
    /**
     * 生成模式：workflow(工作流模式) / agent(Agent自主模式)
     */
    private String genMode;
    /**
     * 部署标识
     */
    private String deployKey;
    /**
     * 部署时间
     */
    private LocalDateTime deployedTime;
    /**
     * 优先级
     */
    private Integer priority;
    /**
     * 创建用户id
     */
    private Long userId;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    /**
     * 归属类型：mine / collaborator
     */
    private String ownershipType;
    /**
     * 创建用户信息
     */
    private UserVO user;
}