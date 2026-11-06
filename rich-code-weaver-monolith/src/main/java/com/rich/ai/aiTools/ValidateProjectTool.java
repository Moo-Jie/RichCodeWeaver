package com.rich.ai.aiTools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.rich.ai.aiTools.fileOperate.CreatAndWriteAiTool;
import com.rich.ai.aiTools.fileOperate.ProjectPathResolver;
import com.rich.app.quality.ProjectQualityGateService;
import com.rich.app.service.AppService;
import com.rich.common.utils.SpringContextUtil;
import com.rich.model.entity.App;
import com.rich.model.enums.CodeGeneratorTypeEnum;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 产物结构验证工具
 * 在 Agent 构建和退出前执行确定性检查，避免模型仅凭自我判断宣告完成
 *
 * @author DuRuiChi
 * @create 2026/11/6
 **/
@Slf4j
@Component
public class ValidateProjectTool extends BaseTool {

    @Override
    public String getToolName() {
        return "validateProject";
    }

    @Override
    public String getToolDisplayName() {
        return "验证产物结构";
    }

    @Override
    public String getResultMsg(JSONObject arguments, String result) {
        return "[工具调用结束] 产物结构验证已执行";
    }

    /**
     * 验证当前产物是否满足结构质量门禁
     *
     * @param appId 产物 ID（自动注入）
     * @return 验证结果
     */
    @Tool("验证产物结构。创建或修改文件后、调用 buildProject 和 exit 前必须调用。")
    public String validateProject(@ToolMemoryId Long appId) {
        if (appId == null || appId <= 0) {
            return "验证失败：产物 ID 无效";
        }
        try {
            String codeGenTypeValue = resolveCodeGenType(appId);
            CodeGeneratorTypeEnum codeGenType = CodeGeneratorTypeEnum.getEnumByValue(codeGenTypeValue);
            if (codeGenType == null) {
                return "验证失败：无法确定代码生成类型";
            }
            ProjectQualityGateService qualityGateService =
                    SpringContextUtil.getBean(ProjectQualityGateService.class);
            ProjectQualityGateService.ValidationResult result = qualityGateService.validate(
                    ProjectPathResolver.resolveProjectRoot(appId).toString(), codeGenType);
            if (result.passed()) {
                return "质量门禁通过：项目结构和本地资源引用完整。下一步请执行 buildProject（Vue 项目）或直接调用 exit。";
            }
            StringBuilder message = new StringBuilder("质量门禁未通过，禁止直接退出：\n");
            result.errors().forEach(error -> message.append("- ").append(error).append("\n"));
            if (!result.suggestions().isEmpty()) {
                message.append("\n修复建议：\n");
                result.suggestions().forEach(suggestion -> message.append("- ").append(suggestion).append("\n"));
            }
            return message.toString();
        } catch (Exception e) {
            log.error("产物结构验证失败: appId={}", appId, e);
            return "验证失败：" + e.getMessage();
        }
    }

    /**
     * 解析当前产物的代码生成类型
     *
     * @param appId 产物 ID
     * @return 代码生成类型
     */
    private String resolveCodeGenType(Long appId) {
        String cachedType = CreatAndWriteAiTool.APP_CODE_GEN_TYPE_CACHE.get(appId);
        if (StrUtil.isNotBlank(cachedType)) {
            return cachedType;
        }
        AppService appService = SpringContextUtil.getBean(AppService.class);
        App app = appService.getById(appId);
        return app == null ? null : app.getCodeGenType();
    }
}
