package com.rich.ai.aiTools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.rich.ai.aiTools.fileOperate.CreatAndWriteAiTool;
import com.rich.ai.aiTools.fileOperate.ProjectPathResolver;
import com.rich.app.quality.ProjectQualityGateService;
import com.rich.app.service.AppService;
import com.rich.common.utils.SpringContextUtil;
import com.rich.common.utils.deployWebProjectUtils.BuildWebProjectExecutor;
import com.rich.model.entity.App;
import com.rich.model.enums.CodeGeneratorTypeEnum;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 退出工具调用
 * 退出前强制执行结构验证和 Vue 构建门禁，不满足条件时拒绝结束 Agent 循环
 *
 * @author DuRuiChi
 * @create 2026/1/29
 **/
@Slf4j
@Component
public class ExitTool extends BaseTool {

    @Override
    public String getToolName() {
        return "exit";
    }

    @Override
    public String getToolDisplayName() {
        return "退出工具调用";
    }

    @Override
    public String getResultMsg(JSONObject arguments, String result) {
        return "\n\n[退出工具执行结果]\n\n" + result;
    }

    /**
     * 执行最终质量门禁，通过后才允许退出
     *
     * @param appId 产物 ID（自动注入）
     * @return 退出确认信息或阻断原因
     */
    @Tool("任务完成前必须调用。工具会执行最终结构验证和 Vue 构建门禁，未通过时禁止退出并返回修复证据。")
    public String exit(@ToolMemoryId Long appId) {
        if (appId == null || appId <= 0) {
            return "退出被阻止：产物 ID 无效";
        }

        try {
            String codeGenTypeValue = resolveCodeGenType(appId);
            CodeGeneratorTypeEnum codeGenType = CodeGeneratorTypeEnum.getEnumByValue(codeGenTypeValue);
            if (codeGenType == null) {
                return "退出被阻止：无法确定代码生成类型，请先调用 setCodeGenType";
            }

            String projectPath = ProjectPathResolver.resolveProjectRoot(appId).toString();
            ProjectQualityGateService qualityGateService =
                    SpringContextUtil.getBean(ProjectQualityGateService.class);
            ProjectQualityGateService.ValidationResult validationResult =
                    qualityGateService.validate(projectPath, codeGenType);
            if (!validationResult.passed()) {
                return buildValidationFailureMessage(validationResult);
            }

            if (CodeGeneratorTypeEnum.VUE_PROJECT.equals(codeGenType)) {
                BuildWebProjectExecutor buildExecutor =
                        SpringContextUtil.getBean(BuildWebProjectExecutor.class);
                BuildWebProjectExecutor.BuildResult buildResult = buildExecutor.buildProjectWithLog(projectPath);
                if (!buildResult.success()) {
                    log.warn("Agent 退出被构建门禁阻止: appId={}, phase={}", appId, buildResult.phase());
                    return "退出被阻止：Vue 项目未通过真实构建。\n"
                            + "失败阶段：" + buildResult.phase() + "\n"
                            + "【构建日志】\n" + buildResult.log() + "\n"
                            + "请根据日志局部修复代码，然后重新调用 buildProject 和 exit。";
                }
            }

            log.info("Agent 最终质量门禁通过，允许退出: appId={}, codeGenType={}",
                    appId, codeGenType.getValue());
            return "质量门禁通过：结构检查、"
                    + (CodeGeneratorTypeEnum.VUE_PROJECT.equals(codeGenType) ? "真实构建" : "静态页面检查")
                    + "均成功。当前任务已经完成，已无需继续调用任何工具，现在输出最终结果。";
        } catch (Exception e) {
            log.error("Agent 退出质量门禁执行失败: appId={}", appId, e);
            return "退出被阻止：最终质量门禁执行异常 - " + e.getMessage();
        }
    }

    /**
     * 构造结构验证失败信息
     *
     * @param validationResult 验证结果
     * @return 阻断信息
     */
    private String buildValidationFailureMessage(ProjectQualityGateService.ValidationResult validationResult) {
        StringBuilder message = new StringBuilder("退出被阻止：项目结构质量门禁未通过。\n");
        validationResult.errors().forEach(error -> message.append("- ").append(error).append("\n"));
        return message.toString();
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
