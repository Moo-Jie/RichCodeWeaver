package com.rich.model.dto.app;

import lombok.Data;

import java.io.Serializable;

@Data
public class AppCodeGenRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 应用id
     */
    private Long appId;
    /**
     * 用户消息
     */
    private String message;
} 