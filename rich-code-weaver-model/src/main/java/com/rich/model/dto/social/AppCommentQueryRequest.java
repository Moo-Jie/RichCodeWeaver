package com.rich.model.dto.social;

import com.rich.common.model.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 产物评论分页查询请求
 *
 * @author DuRuiChi
 * @since 2026-03-25
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AppCommentQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 产物id
     */
    private Long appId;
}
