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
 * 系统提示词 实体类
 *
 * @author DuRuiChi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("system_prompt")
public class SystemPrompt implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id（雪花）
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 提示词名称
     */
    @Column("promptName")
    private String promptName;

    /**
     * 提示词唯一标识（如 html-system-prompt）
     */
    @Column("promptKey")
    private String promptKey;

    /**
     * 提示词内容
     */
    @Column("promptContent")
    private String promptContent;

    /**
     * 提示词描述
     */
    @Column("description")
    private String description;

    /**
     * 创建人id
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
     * 是否删除
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
