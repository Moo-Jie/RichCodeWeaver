package com.rich.codeweaver.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员更新产物请求
 */
@Data
public class AppAdminUpdateRequest implements Serializable {

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
     * 优先级
     */
    private Integer priority;
} 