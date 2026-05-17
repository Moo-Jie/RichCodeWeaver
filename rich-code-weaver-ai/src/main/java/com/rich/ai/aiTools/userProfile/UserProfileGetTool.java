package com.rich.ai.aiTools.userProfile;

import cn.hutool.json.JSONObject;
import com.rich.ai.aiTools.BaseTool;
import com.rich.common.utils.SpringContextUtil;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户画像获取工具
 * 供 LLM Agent 在对话过程中获取当前用户的结构化画像信息
 * 用于个性化代码生成：了解用户偏好的技术栈、风格、项目类型等
 *
 * @author DuRuiChi
 * @create 2026/5/20
 */
@Slf4j
@Component
public class UserProfileGetTool extends BaseTool {

    /**
     * appId → userId 映射缓存
     * 由 CodeGenAgent / CodeGenWorkflowApp 在执行前填充
     * 工具通过此缓存将 @ToolMemoryId(appId) 转换为 userId
     */
    public static final ConcurrentHashMap<Long, Long> APP_USER_MAPPING = new ConcurrentHashMap<>();

    @Override
    public String getToolName() {
        return "getUserProfile";
    }

    @Override
    public String getToolDisplayName() {
        return "获取用户画像";
    }

    @Override
    public String getResultMsg(JSONObject arguments, String result) {
        return "[用户画像] 已获取当前用户画像信息";
    }

    /**
     * 获取当前用户的结构化画像
     *
     * @param appId 产物ID（通过 APP_USER_MAPPING 映射到 userId）
     * @return 用户画像的结构化文本描述
     */
    @Tool("获取当前用户的结构化画像信息，包括偏好的项目类型、技术栈、设计风格、技术水平等。" +
            "在开始生成代码前调用此工具，可以了解用户偏好，从而生成更符合用户期望的代码。")
    public String getUserProfile(@ToolMemoryId Long appId) {
        Long userId = APP_USER_MAPPING.get(appId);
        if (userId == null) {
            log.warn("[UserProfileGetTool] appId={} 未找到对应的 userId 映射", appId);
            return "当前用户暂无画像记录。";
        }

        try {
            // 通过 SpringContextUtil 获取 AiUserProfileService（避免跨模块编译依赖）
            Object profileService = SpringContextUtil.getBean("aiUserProfileServiceImpl");
            java.lang.reflect.Method getByUserIdMethod = profileService.getClass().getMethod("getByUserId", Long.class);
            Object profile = getByUserIdMethod.invoke(profileService, userId);

            if (profile == null) {
                log.info("[UserProfileGetTool] userId={} 暂无画像记录", userId);
                return "当前用户暂无画像记录，这是新用户。你可以在对话中了解用户偏好后，使用 updateUserProfile 工具记录。";
            }

            // 将画像转为可读文本
            return formatProfileToText(profile);
        } catch (Exception e) {
            log.error("[UserProfileGetTool] 获取用户画像失败: appId={}, userId={}", appId, userId, e);
            return "获取用户画像时发生错误: " + e.getMessage();
        }
    }

    /**
     * 将画像对象转为 LLM 可读的结构化文本
     */
    private String formatProfileToText(Object profile) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("【当前用户画像】\n");

            String preferredCodeGenType = getFieldValue(profile, "getPreferredCodeGenType");
            String preferredGenMode = getFieldValue(profile, "getPreferredGenMode");
            String techPreferences = getFieldValue(profile, "getTechPreferences");
            String stylePreferences = getFieldValue(profile, "getStylePreferences");
            String dislikedPatterns = getFieldValue(profile, "getDislikedPatterns");
            String usageGoal = getFieldValue(profile, "getUsageGoal");
            String experienceLevel = getFieldValue(profile, "getExperienceLevel");
            String preferredLanguage = getFieldValue(profile, "getPreferredLanguage");
            String customNotes = getFieldValue(profile, "getCustomNotes");

            if (preferredCodeGenType != null) sb.append("- 偏好项目类型: ").append(preferredCodeGenType).append("\n");
            if (preferredGenMode != null) sb.append("- 偏好生成模式: ").append(preferredGenMode).append("\n");
            if (techPreferences != null) sb.append("- 技术栈偏好: ").append(techPreferences).append("\n");
            if (stylePreferences != null) sb.append("- 设计风格偏好: ").append(stylePreferences).append("\n");
            if (dislikedPatterns != null) sb.append("- 不喜欢的模式: ").append(dislikedPatterns).append("\n");
            if (usageGoal != null) sb.append("- 使用目的: ").append(usageGoal).append("\n");
            if (experienceLevel != null) sb.append("- 技术水平: ").append(experienceLevel).append("\n");
            if (preferredLanguage != null) sb.append("- 偏好语言: ").append(preferredLanguage).append("\n");
            if (customNotes != null) sb.append("- 备注: ").append(customNotes).append("\n");

            // 如果所有字段都为空，提示是空画像
            if (sb.toString().equals("【当前用户画像】\n")) {
                return "当前用户画像记录存在但所有字段为空，你可以在对话中了解用户偏好后，使用 updateUserProfile 工具记录。";
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("[UserProfileGetTool] 格式化画像失败", e);
            return "用户画像已找到但格式化失败。";
        }
    }

    /**
     * 通过反射获取字段值（避免跨模块编译依赖）
     */
    private String getFieldValue(Object obj, String methodName) {
        try {
            java.lang.reflect.Method method = obj.getClass().getMethod(methodName);
            Object value = method.invoke(obj);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
