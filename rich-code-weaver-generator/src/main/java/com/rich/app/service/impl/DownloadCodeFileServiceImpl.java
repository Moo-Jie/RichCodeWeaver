package com.rich.app.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import com.rich.app.service.DownloadCodeFileService;
import com.rich.common.constant.AppConstant;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.model.entity.App;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileFilter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

/**
 * 代码文件下载服务实现类
 * 提供将生成的代码打包为 ZIP 文件并下载的功能
 *
 * @author DuRuiChi
 * @since 2026-03-10
 */
@Slf4j
@Service
public class DownloadCodeFileServiceImpl implements DownloadCodeFileService {
    /**
     * 需要过滤的文件和目录名称
     */
    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules",
            ".git",
            "dist",
            "build",
            ".DS_Store",
            ".env",
            "target",
            ".mvn",
            ".idea",
            ".vscode",
            // Vue项目特有过滤项
            ".nuxt",           // Nuxt.js 构建目录
            ".output",         // Nuxt.js 输出目录
            ".vuepress",       // VuePress 构建目录
            ".cache",          // 缓存目录
            "coverage",        // 测试覆盖率报告
            ".nyc_output",     // NYC 测试覆盖率输出
            "storybook-static", // Storybook 静态文件
            ".temp",           // 临时文件目录
            ".turbo"           // Turbo 构建缓存
    );

    /**
     * 需要过滤的文件扩展名
     */
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log",
            ".template",
            ".cache",
            // Vue项目特有过滤项
            ".map",            // Source map 文件
            ".tsbuildinfo",    // TypeScript 构建信息
            ".vue.ts",         // Vue 单文件组件的 TypeScript 声明文件
            ".vue.js",         // Vue 单文件组件的 JavaScript 声明文件
            ".d.ts",           // TypeScript 声明文件
            ".d.ts.map",       // TypeScript 声明文件的 source map
            ".spec.js",        // 测试文件
            ".test.js",        // 测试文件
            ".spec.ts",        // TypeScript 测试文件
            ".test.ts",        // TypeScript 测试文件
            ".spec.vue",       // Vue 测试文件
            ".test.vue"        // Vue 测试文件
    );

    /**
     * 路径过滤（保证下载最纯净的代码）
     * 过滤掉不必要的文件和目录，如 node_modules、.git、dist 等
     *
     * @param projectRoot 项目根目录
     * @param fullPath    完整路径
     * @return boolean 是否允许该路径通过过滤
     * @author DuRuiChi
     */
    private boolean pathFiltering(Path projectRoot, Path fullPath) {
        // 参数校验：确保路径参数有效
        if (projectRoot == null || fullPath == null) {
            log.warn("路径过滤失败：路径参数为空");
            return false;
        }

        try {
            // 计算相对于项目根目录的相对路径
            Path relativePath = projectRoot.relativize(fullPath);

            // 遍历路径的每一部分，检查是否需要过滤
            for (Path pathSegment : relativePath) {
                String segmentName = pathSegment.toString();

                // 空值保护
                if (segmentName == null || segmentName.isEmpty()) {
                    continue;
                }

                // 检查目录名称是否在过滤列表中（如 node_modules、.git 等）
                if (IGNORED_NAMES.contains(segmentName)) {
                    log.debug("过滤目录: {}", segmentName);
                    return false;  // 过滤掉该路径
                }

                // 只对文件进行扩展名检查（路径的最后一部分才是文件名）
                if (pathSegment.equals(relativePath.getFileName())) {
                    // 将文件名转换为小写进行大小写不敏感的扩展名匹配
                    String fileNameLowerCase = segmentName.toLowerCase();

                    // 检查文件扩展名是否在过滤列表中（如 .log、.map 等）
                    boolean shouldFilter = IGNORED_EXTENSIONS.stream()
                            .anyMatch(fileNameLowerCase::endsWith);
                    if (shouldFilter) {
                        log.debug("过滤文件: {}", segmentName);
                        return false;  // 过滤掉该文件
                    }
                }
            }

            // 路径通过过滤，允许打包
            return true;
        } catch (Exception e) {
            log.error("路径过滤异常: projectRoot={}, fullPath={}, error={}",
                    projectRoot, fullPath, e.getMessage());
            return false;
        }
    }

    /**
     * 下载代码压缩包文件
     * 将指定产物的代码目录打包为 ZIP 文件并通过 HTTP 响应下载
     *
     * @param app      产物实体
     * @param response HTTP 响应对象
     * @throws BusinessException 如果压缩或下载失败
     * @author DuRuiChi
     */
    @Override
    public void downloadCodeZipFile(App app, HttpServletResponse response) {
        // 参数校验：确保产物对象和响应对象有效
        if (app == null) {
            log.error("下载代码压缩包失败：产物对象为空");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "产物对象为空");
        }
        if (response == null) {
            log.error("下载代码压缩包失败：响应对象为空 - appId={}", app.getId());
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "响应对象为空");
        }

        Long appId = app.getId();
        String codeGenType = app.getCodeGenType();

        // 步骤1：构建代码输出目录的绝对路径并校验
        String absoluteDirPath = buildCodeAbsolutePath(app);
        File projectDir = new File(absoluteDirPath);

        // 再次校验目录是否存在且为目录（双重保护）
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("下载代码压缩包失败：项目目录不存在或不是目录 - appId={}, path={}",
                    appId, absoluteDirPath);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "项目目录不存在，请先生成代码");
        }

        // 步骤2：构建压缩包文件名（格式：appId_codeGenType.zip）
        String zipFileName = appId + "_" + codeGenType;
        log.info("开始下载代码压缩包: appId={}, zipFileName={}, projectDir={}",
                appId, zipFileName, absoluteDirPath);

        // 步骤3：设置 HTTP 响应头，告诉浏览器这是一个下载文件
        try {
            response.setStatus(HttpServletResponse.SC_OK);  // 设置状态码为 200 OK
            response.setContentType("application/zip");  // 设置内容类型为 ZIP 文件
            // 设置 Content-Disposition 头，指定文件名（触发浏览器下载）
            response.addHeader("Content-Disposition",
                    String.format("attachment; filename=\"%s.zip\"", zipFileName));
        } catch (Exception e) {
            log.error("设置HTTP响应头失败: appId={}, error={}", appId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "设置HTTP响应头失败");
        }

        // 步骤4：创建文件过滤器，过滤掉不必要的文件和目录
        FileFilter fileFilter = file -> pathFiltering(projectDir.toPath(), file.toPath());

        // 步骤5：压缩项目目录并直接写入 HTTP 响应流
        try {
            // 使用 Hutool 工具包的 ZipUtil.zip() 方法进行压缩
            ZipUtil.zip(
                    response.getOutputStream(),  // 直接写入 HTTP 响应流（流式传输，节省内存）
                    StandardCharsets.UTF_8,      // 使用 UTF-8 编码（防止中文文件名乱码）
                    false,                       // 不递归压缩子目录（由过滤器控制递归逻辑）
                    fileFilter,                  // 文件过滤器（过滤node_modules等不必要文件）
                    projectDir);                 // 要压缩的项目目录

            log.info("项目代码文件压缩包下载完成: appId={}, zipFileName={}", appId, zipFileName);
        } catch (Exception e) {
            // 记录错误日志并抛出业务异常
            log.error("项目代码文件压缩包下载失败: appId={}, zipFileName={}, error={}",
                    appId, zipFileName, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "项目代码文件压缩包下载失败： " + e.getMessage());
        }
    }

    /**
     * 构建代码绝对路径
     * 根据产物信息构建代码输出目录的绝对路径并进行校验
     *
     * @param app 产物实体
     * @return String 代码绝对路径
     * @throws BusinessException 如果路径不存在或不是目录
     * @author DuRuiChi
     */
    private String buildCodeAbsolutePath(App app) {
        // 参数校验：确保产物对象有效
        if (app == null) {
            log.error("构建代码路径失败：产物对象为空");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "产物对象为空");
        }

        Long appId = app.getId();
        String codeGenType = app.getCodeGenType();

        // 校验产物ID和代码生成类型
        if (appId == null || appId <= 0) {
            log.error("构建代码路径失败：产物ID无效 - appId={}", appId);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "产物ID无效");
        }
        if (StrUtil.isBlank(codeGenType)) {
            log.error("构建代码路径失败：代码生成类型为空 - appId={}", appId);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码生成类型为空");
        }

        // 构建代码输出目录的绝对路径（格式：根目录/codeGenType_appId）
        String absoluteDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR
                + File.separator
                + codeGenType
                + "_"
                + appId;

        // 路径校验：确保目录存在且是有效目录
        File sourceDir = new File(absoluteDirPath);

        // 校验目录是否存在
        if (!sourceDir.exists()) {
            log.error("构建代码路径失败：目录不存在 - appId={}, path={}", appId, absoluteDirPath);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,
                    "项目资源所在目录不存在，请先生成代码");
        }

        // 校验是否为目录
        if (!sourceDir.isDirectory()) {
            log.error("构建代码路径失败：路径不是目录 - appId={}, path={}", appId, absoluteDirPath);
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    "项目资源路径异常，请联系管理员");
        }

        log.debug("构建代码路径成功: appId={}, path={}", appId, absoluteDirPath);
        return absoluteDirPath;
    }
}
