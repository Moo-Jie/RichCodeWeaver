package com.rich.common.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 删除产物响应�?
 *
 * @author DuRuiChi
 * @since 2026-03-08
 **/
@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 产物 id
     */
    private Long id;
}
