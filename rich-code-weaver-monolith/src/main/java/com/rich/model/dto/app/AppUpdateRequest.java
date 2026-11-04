package com.rich.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新产物请求
 */
@Data
public class AppUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;
    /**
     * 产物名称
     */
    private String appName;
} 