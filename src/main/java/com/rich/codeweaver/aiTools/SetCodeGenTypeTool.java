package com.rich.codeweaver.aiTools;

import cn.hutool.json.JSONObject;
import com.mybatisflex.core.update.UpdateChain;
import com.rich.codeweaver.aiTools.fileOperate.CreatAndWriteAiTool;
import com.rich.codeweaver.model.entity.App;
import com.rich.codeweaver.model.enums.CodeGeneratorTypeEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Agent 工作流工具：设置并持久化代码生成类型
 * Agent 在分析用户需求后，调用此工具确定最终技术方案并写入数据库。
 * 调用后，文件创建工具将使用对应类型的项目目录。
 *
 * @author DuRuiChi
 * @create 2026/4/1
 **/
@Slf4j
@Component
public class SetCodeGenTypeTool extends BaseTool {

    @Override
    public String getToolName() {
        return "setCodeGenType";
    }

    @Override
    public String getToolDisplayName() {
        return "设置代码生成类型";
    }

    @Override
    public String getResultMsg(JSONObject arguments, String result) {
        String type = arguments.getStr("codeGenType");
        return "[工具调用结束] 代码生成类型已设置为: " + type;
    }

    /**
     * 设置并持久化代码生成类型
     * 在技术方案分析阶段完成后立即调用，后续文件创建工具将使用对应目录
     *
     * @param codeGenType 代码生成类型
     * @param appId       产物 ID（自动注入，无需手动传入）
     * @return 操作结果与后续行动指引
     */
    @Tool("【阶段二必须调用】分析用户需求，确定技术方案后调用此工具设置代码生成类型并持久化到数据库")
    public String setCodeGenType(
            @P("代码生成类型，必须是以下三者之一：\n" +
               "- single_html: 单 HTML 文件，适合简单展示页、活动页（不需要构建）\n" +
               "- multi_file: 多文件结构（index.html + style.css + script.js），适合中等复杂度（不需要构建）\n" +
               "- vue_project: Vue3 工程项目，适合多页面、路由、组件化的复杂应用（需要构建）")
            String codeGenType,
            @ToolMemoryId Long appId) {

        // 支持 'html' 作为 'single_html' 的别名（兼容 AI 可能使用的简化名称）
        String normalizedType = "html".equalsIgnoreCase(codeGenType) ? "single_html" : codeGenType;
        CodeGeneratorTypeEnum typeEnum = CodeGeneratorTypeEnum.getEnumByValue(normalizedType);
        if (typeEnum == null) {
            String msg = "错误: 不支持的代码生成类型 '" + codeGenType
                    + "'，请使用: html / multi_file / vue_project";
            log.warn(msg);
            return msg;
        }

        // 更新数据库中的代码生成类型（使用规范化后的值）
        boolean updated = UpdateChain.of(App.class)
                .set(App::getCodeGenType, normalizedType)
                .eq(App::getId, appId)
                .update();

        if (!updated) {
            String msg = "警告: 数据库更新失败，appId=" + appId + "，类型=" + normalizedType + "，请继续执行代码生成";
            log.warn(msg);
            return msg;
        }

        log.info("代码生成类型已设置: appId={}, codeGenType={}", appId, normalizedType);

        // 更新 creatAndWrite 工具的类型缓存，确保后续文件写入正确的目录
        CreatAndWriteAiTool.APP_CODE_GEN_TYPE_CACHE.put(appId, normalizedType);

        // 根据类型给出后续文件创建指引（目录名 = codeGenType_appId）
        return switch (typeEnum) {
            case HTML -> """
                    代码生成类型已设置为 single_html。
                    【后续操作指引】
                    - 文件目录: 所有文件写入 single_html_%d/ 目录（creatAndWrite 工具使用相对路径自动定位）
                    - 必须创建: index.html（包含完整的 HTML + 内联 CSS/JS，单文件即可运行）
                    - 无需构建: single_html 类型直接提供文件服务，不需要调用 buildProject 工具
                    - 完成后: 调用 readDir 自检，然后调用 exit 结束任务
                    """.formatted(appId);
            case MULTI_FILE -> """
                    代码生成类型已设置为 multi_file。
                    【后续操作指引】
                    - 文件目录: 所有文件写入 multi_file_%d/ 目录（creatAndWrite 工具使用相对路径自动定位）
                    - 推荐创建: index.html、style.css、script.js（相互正确引用路径）
                    - 无需构建: multi_file 类型直接提供文件服务，不需要调用 buildProject 工具
                    - 完成后: 调用 readDir 自检，然后调用 exit 结束任务
                    """.formatted(appId);
            case VUE_PROJECT -> """
                    代码生成类型已设置为 vue_project。
                    【后续操作指引】
                    - 文件目录: 所有文件写入 vue_project_%d/ 目录（creatAndWrite 工具使用相对路径自动定位）
                    - 必须创建: package.json、vite.config.js、index.html、src/main.js、src/App.vue、src/router/index.js
                    - 必须构建: 所有文件创建完毕后，必须调用 buildProject 工具进行构建
                    - 构建成功后: 调用 exit 结束任务
                    """.formatted(appId);
            default -> "代码生成类型已设置为 " + normalizedType;
        };
    }
}
