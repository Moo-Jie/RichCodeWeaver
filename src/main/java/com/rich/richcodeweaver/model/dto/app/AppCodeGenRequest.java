package com.rich.richcodeweaver.model.dto.app;

import lombok.Data;

import java.io.Serializable;

@Data
public class AppCodeGenRequest implements Serializable {

    /**
     * 应用id
     */
    private Long appId;

    /**
     * 用户消息
     */
    private String message;

    private static final long serialVersionUID = 1L;
} 