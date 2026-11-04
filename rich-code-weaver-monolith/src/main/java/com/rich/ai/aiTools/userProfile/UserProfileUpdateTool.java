package com.rich.ai.aiTools.userProfile;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rich.ai.aiTools.BaseTool;
import com.rich.common.utils.SpringContextUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户画像更新工具
 * 供 LLM Agent 在对话过程中提取、记录用户偏好信息
 * 支持增量更新：仅覆盖本次传入的非空字段，不影响已有画像数据
 *
 * @author DuRuiChi
 * @create 2026/5/20
 */
@Slf4j
@Component
public class UserProfileUpdateTool extends BaseTool {

    @Override
    public String getToolName() {
        return "updateUserProfile";
    }

    @Override
    public String getToolDisplayName() {
        return "更新用户画像";
    }

    @Override
    public String getResultMsg(JSONObject arguments, String result) {
        return "[用户画像] 已更新当前用户画像信息";
    }

    /**
     * 更新当前用户的结构化画像（增量更新，仅覆盖传入的非空字段）
     *
     * @param preferredCodeGenType 偏好的项目类型（可选）
     * @param preferredGenMode     偏好的生成模式（可选）
     * @param techPreferences      技术栈偏好（可选）
     * @param stylePreferences     设计风格偏好（可选）
     * @param dislikedPatterns     不喜欢的模式（可选）
     * @param usageGoal            使用目的（可选）
     * @param experienceLevel      技术水平（可选）
     * @param preferredLanguage    偏好语言（可选）
     * @param customNotes          自由备注（可选）
     * @param appId                产物ID
     * @return 更新结果
     */
    @Tool("更新当前用户的结构化画像信息。增量更新，仅传入需要更新的字段即可，未传入的字段不会被覆盖。" +
            "当从对话中发现用户的偏好、习惯、技术水平等信息时，调用此工具记录下来。")
    public String updateUserProfile(
            @P("偏好的项目类型: single_html（单页HTML）/ multi_file（多文件项目）/ vue_project（Vue工程项目），可选")
            String preferredCodeGenType,
            @P("偏好的生成模式: agent（Agent自主模式）/ workflow（工作流模式），可选")
            String preferredGenMode,
            @P("技术栈偏好，例如：偏好 Vue3 + TailwindCSS、喜欢用 Element Plus 等，可选")
            String techPreferences,
            @P("UI/设计风格偏好，例如：简约现代风、暗色主题、喜欢圆角卡片等，可选")
            String stylePreferences,
            @P("不喜欢的模式/风格，例如：不喜欢过多动画、避免使用 jQuery 等，可选")
            String dislikedPatterns,
            @P("使用目的，例如：个人作品集、企业官网、学习项目等，可选")
            String usageGoal,
            @P("技术水平: beginner（初学者）/ intermediate（中级）/ advanced（高级），可选")
            String experienceLevel,
            @P("偏好语言，例如：中文、英文，可选")
            String preferredLanguage,
            @P("自由备注，可记录其他有用的用户信息，可选")
            String customNotes,
            @ToolMemoryId Long appId) {

        Long userId = UserProfileGetTool.APP_USER_MAPPING.get(appId);
        if (userId == null) {
            log.warn("[UserProfileUpdateTool] appId={} 未找到对应的 userId 映射", appId);
            return "错误：无法确定当前用户，跳过画像更新。";
        }

        try {
            // 构建更新数据（通过反射调用，避免跨模块编译依赖）
            JSONObject profileData = new JSONObject();
            if (preferredCodeGenType != null && !preferredCodeGenType.isBlank()) {
                profileData.set("preferredCodeGenType", preferredCodeGenType.trim());
            }
            if (preferredGenMode != null && !preferredGenMode.isBlank()) {
                profileData.set("preferredGenMode", preferredGenMode.trim());
            }
            if (techPreferences != null && !techPreferences.isBlank()) {
                profileData.set("techPreferences", techPreferences.trim());
            }
            if (stylePreferences != null && !stylePreferences.isBlank()) {
                profileData.set("stylePreferences", stylePreferences.trim());
            }
            if (dislikedPatterns != null && !dislikedPatterns.isBlank()) {
                profileData.set("dislikedPatterns", dislikedPatterns.trim());
            }
            if (usageGoal != null && !usageGoal.isBlank()) {
                profileData.set("usageGoal", usageGoal.trim());
            }
            if (experienceLevel != null && !experienceLevel.isBlank()) {
                profileData.set("experienceLevel", experienceLevel.trim());
            }
            if (preferredLanguage != null && !preferredLanguage.isBlank()) {
                profileData.set("preferredLanguage", preferredLanguage.trim());
            }
            if (customNotes != null && !customNotes.isBlank()) {
                profileData.set("customNotes", customNotes.trim());
            }

            if (profileData.isEmpty()) {
                return "未提供任何需要更新的字段，画像未变更。";
            }

            // 通过 SpringContextUtil 获取服务并调用
            Object profileService = SpringContextUtil.getBean("aiUserProfileServiceImpl");

            // 获取 AiUserProfile 类（运行时）
            Class<?> profileClass = Class.forName("com.rich.model.entity.AiUserProfile");
            Object profileEntity = JSONUtil.toBean(profileData.toString(), profileClass);

            // 调用 createOrUpdate(Long userId, AiUserProfile profile)
            java.lang.reflect.Method createOrUpdateMethod = profileService.getClass()
                    .getMethod("createOrUpdate", Long.class, profileClass);
            createOrUpdateMethod.invoke(profileService, userId, profileEntity);

            log.info("[UserProfileUpdateTool] 用户画像已更新: appId={}, userId={}, fields={}",
                    appId, userId, profileData.keySet());

            // 构建更新确认消息
            StringBuilder sb = new StringBuilder();
            sb.append("用户画像已成功更新，本次更新的字段：\n");
            for (String key : profileData.keySet()) {
                sb.append("- ").append(getFieldDisplayName(key)).append(": ").append(profileData.getStr(key)).append("\n");
            }
            return sb.toString();

        } catch (Exception e) {
            log.error("[UserProfileUpdateTool] 更新用户画像失败: appId={}, userId={}", appId, userId, e);
            return "更新用户画像时发生错误: " + e.getMessage();
        }
    }

    /**
     * 字段名 → 中文显示名
     */
    private String getFieldDisplayName(String fieldName) {
        return switch (fieldName) {
            case "preferredCodeGenType" -> "偏好项目类型";
            case "preferredGenMode" -> "偏好生成模式";
            case "techPreferences" -> "技术栈偏好";
            case "stylePreferences" -> "设计风格偏好";
            case "dislikedPatterns" -> "不喜欢的模式";
            case "usageGoal" -> "使用目的";
            case "experienceLevel" -> "技术水平";
            case "preferredLanguage" -> "偏好语言";
            case "customNotes" -> "自由备注";
            default -> fieldName;
        };
    }
}
