package com.rich.codeweaver.model.dto.friendship;

import com.rich.codeweaver.common.model.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 好友列表查询请求
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FriendQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 搜索关键词（按用户名模糊搜索）
     */
    private String keyword;
}
