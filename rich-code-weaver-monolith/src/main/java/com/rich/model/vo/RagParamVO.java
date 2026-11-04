package com.rich.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * RAG 参数配置 VO
 *
 * @author DuRuiChi
 * @create 2026/3/27
 */
@Data
public class RagParamVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 参数键（唯一标识）
     */
    private String paramKey;

    /**
     * 参数名称（中文）
     */
    private String paramName;

    /**
     * 参数值
     */
    private String paramValue;

    /**
     * 参数类型：int / double / textarea
     */
    private String paramType;

    /**
     * 参数分组：etl / retrieval / injection
     */
    private String paramGroup;

    /**
     * 参数描述
     */
    private String description;

    /**
     * 排序
     */
    private Integer sortOrder;

    private LocalDateTime updateTime;
}
