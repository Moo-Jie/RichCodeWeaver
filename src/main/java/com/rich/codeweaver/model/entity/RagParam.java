package com.rich.codeweaver.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * RAG 参数配置实体类
 * 存储 RAG 管道中各阶段的可配置参数，替代代码中写死的常量
 *
 * @author DuRuiChi
 * @create 2026/3/27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("rag_param")
public class RagParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id（雪花）
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 参数键（唯一标识，代码中使用）
     * 例如：max_segment_size、min_score、injection_prompt_template
     */
    @Column("param_key")
    private String paramKey;

    /**
     * 参数名称（中文，供管理界面展示）
     */
    @Column("param_name")
    private String paramName;

    /**
     * 参数值（统一以字符串存储，按 param_type 转换）
     */
    @Column("param_value")
    private String paramValue;

    /**
     * 参数类型：int / double / textarea
     */
    @Column("param_type")
    private String paramType;

    /**
     * 参数分组：etl / retrieval / injection
     */
    @Column("param_group")
    private String paramGroup;

    /**
     * 参数描述（用途说明）
     */
    @Column("description")
    private String description;

    /**
     * 排序（越小越靠前）
     */
    @Column("sort_order")
    private Integer sortOrder;

    /**
     * 创建时间
     */
    @Column("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column("update_time")
    private LocalDateTime updateTime;
}
