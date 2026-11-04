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
 * AI 结构化用户画像 实体类
 * 按 userId 唯一标识，永久存储用户偏好信息，供 LLM Agent 在生成代码时参考
 *
 * @author DuRuiChi
 * @create 2026/5/20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("ai_user_profile")
public class AiUserProfile implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id（雪花）
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 用户 ID（唯一标识，一个用户只有一条画像记录）
     */
    @Column("userId")
    private Long userId;

    /**
     * 偏好的项目类型（如 single_html / multi_file / vue_project）
     */
    @Column("preferredCodeGenType")
    private String preferredCodeGenType;

    /**
     * 偏好的生成模式（agent / workflow）
     */
    @Column("preferredGenMode")
    private String preferredGenMode;

    /**
     * 技术栈偏好（如 "偏好 Vue3 + TailwindCSS"、"喜欢用 Element Plus"）
     */
    @Column("techPreferences")
    private String techPreferences;

    /**
     * UI/设计风格偏好（如 "简约现代风"、"暗色主题"、"喜欢圆角卡片"）
     */
    @Column("stylePreferences")
    private String stylePreferences;

    /**
     * 不喜欢的模式/风格（如 "不喜欢过多动画"、"避免使用 jQuery"）
     */
    @Column("dislikedPatterns")
    private String dislikedPatterns;

    /**
     * 使用目的（如 "个人作品集"、"企业官网"、"学习项目"）
     */
    @Column("usageGoal")
    private String usageGoal;

    /**
     * 技术水平：beginner / intermediate / advanced
     */
    @Column("experienceLevel")
    private String experienceLevel;

    /**
     * 偏好语言（如 "中文"、"英文"）
     */
    @Column("preferredLanguage")
    private String preferredLanguage;

    /**
     * 自由备注（Agent 可自由记录的额外信息）
     */
    @Column("customNotes")
    private String customNotes;

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
