package com.rich.common.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 删除应用响应类
 *
 * @author DuRuiChi
 * @create 2025/8/4
 **/
@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 应用 id
     */
    private Long id;
}