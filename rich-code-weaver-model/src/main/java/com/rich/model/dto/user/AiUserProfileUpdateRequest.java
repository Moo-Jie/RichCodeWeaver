package com.rich.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class AiUserProfileUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String preferredCodeGenType;

    private String preferredGenMode;

    private String techPreferences;

    private String stylePreferences;

    private String dislikedPatterns;

    private String usageGoal;

    private String experienceLevel;

    private String preferredLanguage;

    private String customNotes;
}
