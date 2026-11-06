package com.rich.ai.aiTools.fileOperate;

import com.rich.common.constant.AppConstant;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 项目文件路径解析器
 * 统一限制 AI 文件工具只能访问当前产物目录，禁止绝对路径和目录穿越
 *
 * @author DuRuiChi
 * @create 2026/11/6
 **/
public final class ProjectPathResolver {

    /**
     * 默认代码生成类型
     */
    private static final String DEFAULT_CODE_GEN_TYPE = "vue_project";

    /**
     * 工具类禁止实例化
     */
    private ProjectPathResolver() {
    }

    /**
     * 解析项目根目录
     *
     * @param appId 产物 ID
     * @return 项目根目录绝对路径
     */
    public static Path resolveProjectRoot(Long appId) {
        String codeGenType = CreatAndWriteAiTool.APP_CODE_GEN_TYPE_CACHE
                .getOrDefault(appId, DEFAULT_CODE_GEN_TYPE);
        return Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, codeGenType + "_" + appId)
                .toAbsolutePath()
                .normalize();
    }

    /**
     * 解析项目内相对路径
     *
     * @param appId        产物 ID
     * @param relativePath 相对路径
     * @return 项目内绝对路径
     */
    public static Path resolve(Long appId, String relativePath) {
        Path projectRoot = resolveProjectRoot(appId);
        Path requestedPath = Paths.get(relativePath == null ? "" : relativePath.trim());
        if (requestedPath.isAbsolute()) {
            throw new IllegalArgumentException("禁止使用绝对路径，请传入项目内相对路径");
        }
        Path resolvedPath = projectRoot.resolve(requestedPath).normalize();
        if (!resolvedPath.startsWith(projectRoot)) {
            throw new IllegalArgumentException("路径越界，文件工具只能访问当前项目目录");
        }
        return resolvedPath;
    }
}
