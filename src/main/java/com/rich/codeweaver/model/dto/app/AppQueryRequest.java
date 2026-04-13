package com.rich.codeweaver.model.dto.app;

import com.rich.codeweaver.common.model.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class AppQueryRequest extends PageRequest implements Serializable {

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
     * 部署标识
     */
    private String deployKey;
    /**
     * 优先级
     */
    private Integer priority;
    /**
     * 创建用户id
     */
    private Long userId;
} 