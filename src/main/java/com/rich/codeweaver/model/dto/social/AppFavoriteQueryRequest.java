package com.rich.codeweaver.model.dto.social;

import com.rich.codeweaver.common.model.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 我的收藏分页查询请求
 *
 * @author DuRuiChi
 * @since 2026-03-25
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AppFavoriteQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;
}
