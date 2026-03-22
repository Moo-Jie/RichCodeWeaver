package com.rich.common.model;

import lombok.Data;

/**
 * 分页响应�?
 *
 * @author DuRuiChi
 * @since 2026-03-08
 **/
@Data
public class PageRequest {

    /**
     * 当前页号
     */
    private int pageNum = 1;

    /**
     * 页面大小
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认降序）
     */
    private String sortOrder = "descend";
}
