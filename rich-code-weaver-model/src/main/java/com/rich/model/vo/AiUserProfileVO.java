package com.rich.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AiUserProfileVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String preferredCodeGenType;

    private String preferredGenMode;

    private String techPreferences;

    private String stylePreferences;

    private String dislikedPatterns;

    private String usageGoal;

    private String experienceLevel;

    private String preferredLanguage;

    private String customNotes;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
