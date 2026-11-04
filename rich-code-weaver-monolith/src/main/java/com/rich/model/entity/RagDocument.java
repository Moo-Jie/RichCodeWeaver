package com.rich.model.entity;

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
 * RAG 知识库文档实体类
 * 存储用于检索增强生成（RAG）的知识库文档，替代原来从文件系统读取的方式
 *
 * @author DuRuiChi
 * @create 2026/3/27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("rag_document")
public class RagDocument implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id（雪花）
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 文档业务类型
     * CODE_GEN / CUSTOMER_SERVICE
     */
    @Column("bizType")
    private String bizType;

    /**
     * 文档标题
     */
    @Column("docTitle")
    private String docTitle;

    /**
     * 文档正文内容（Markdown 格式）
     * 当前知识库文档约 8-10KB，使用 TEXT 类型足够（MySQL TEXT 上限 ~64KB，PostgreSQL TEXT 无限制）
     */
    @Column("docContent")
    private String docContent;

    /**
     * 适用的代码生成类型
     * HTML / MULTI_FILE / VUE_PROJECT / GENERAL
     * GENERAL 表示所有类型均可使用
     */
    @Column("codeGenType")
    private String codeGenType;

    /**
     * 文档描述（简短说明，供管理界面展示）
     */
    @Column("description")
    private String description;

    /**
     * 是否启用（1-启用，0-禁用）
     * 禁用的文档不会被摄入向量库
     */
    @Column("isEnabled")
    private Integer isEnabled;

    /**
     * 排序权重（越小越靠前）
     */
    @Column("sortOrder")
    private Integer sortOrder;

    /**
     * 创建人 id
     */
    @Column("userId")
    private Long userId;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column("updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除（逻辑删除）
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
