package com.rich.app.service.impl;

import cn.hutool.core.util.ZipUtil;
import com.rich.app.service.DownloadCodeFileService;
import com.rich.common.constant.AppConstant;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.model.entity.App;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileFilter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

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
            ".tmp",
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
     *
     * @param projectRoot 项目根目录
     * @param fullPath    完整路径
     * @return 是否允许
     */
    private boolean pathFiltering(Path projectRoot, Path fullPath) {
        // 获取相对路径
        Path relativePath = projectRoot.relativize(fullPath);

        // 过滤路径
        for (Path pathBlock : relativePath) {
            String pathBlockName = pathBlock.toString();

            // 过滤掉目录名称
            if (IGNORED_NAMES.contains(pathBlockName)) {
                return false;
            }

            // 只对文件进行扩展名检查（路径的最后一部分）
            if (pathBlock.equals(relativePath.getFileName())) {
                // 扩展名检查
                String fileName = pathBlockName.toLowerCase();
                if (IGNORED_EXTENSIONS.stream().anyMatch(fileName::endsWith)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void downloadCodeZipFile(App app, HttpServletResponse response) {
        // 构建代码输出目录的绝对路径
        String absoluteDirPath = BuildCodeAbsolutePath(app);
        // 代码输出目录
        File projectDir = new File(absoluteDirPath);

        // 构建压缩包名称
        String zipFileName = app.getId().toString() + "_" + app.getCodeGenType();

        // 设置 HTTP 响应头
        // 设置 HTTP 状态码为 200 OK
        response.setStatus(HttpServletResponse.SC_OK);
        // 设置响应内容类型为 ZIP 文件
        response.setContentType("application/zip");
        // 添加 Content-Disposition 头，指定文件名
        response.addHeader("Content-Disposition",
                String.format("attachment; filename=\"%s.zip\"", zipFileName));

        // 生成文件过滤器,过滤掉一些冗余文件和目录
        FileFilter fileFilter = file -> pathFiltering(projectDir.toPath(), file.toPath());

        // 压缩并触发下载,通过 response.getOutputStream() 直接写入响应内容
        try {
            // Hutool 工具包的 ZipUtil.zip() 方法
            ZipUtil.zip(response.getOutputStream(),
                    // 字符编码
                    StandardCharsets.UTF_8,
                    // 是否递归压缩子目录
                    false,
                    // 文件过滤器
                    fileFilter,
                    // 要压缩的目录
                    projectDir);
            log.info("项目代码文件压缩包下载完成: {}", zipFileName);
        } catch (Exception e) {
            log.error("项目代码文件压缩包下载失败，因为: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "项目代码文件压缩包下载失败，因为： " + e.getMessage());
        }
    }

    /**
     * 构建代码绝对路径
     *
     * @param app 应用
     * @return 代码绝对路径
     */
    private String BuildCodeAbsolutePath(App app) {
        // 代码输出目录的绝对路径
        String absoluteDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + app.getCodeGenType() + "_" + app.getId();
        // 路径校验
        File sourceDir = new File(absoluteDirPath);
        ThrowUtils.throwIf(!sourceDir.isDirectory(), ErrorCode.PARAMS_ERROR, "项目资源路径拼装失败，请联系管理员");
        ThrowUtils.throwIf(!sourceDir.exists(), ErrorCode.NOT_FOUND_ERROR, "项目资源所在目录不存在，请联系管理员");
        return absoluteDirPath;
    }
}
