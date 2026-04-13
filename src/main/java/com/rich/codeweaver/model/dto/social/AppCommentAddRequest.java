package com.rich.codeweaver.model.dto.social;

import lombok.Data;

import java.io.Serializable;

/**
 * 产物评论新增请求
 *
 * @author DuRuiChi
 * @since 2026-03-25
 */
@Data
public class AppCommentAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 产物id
     */
    private Long appId;

    /**
     * 评论内容
     */
    private String content;
}
