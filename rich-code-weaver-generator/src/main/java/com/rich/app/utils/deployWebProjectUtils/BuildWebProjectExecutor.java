package com.rich.app.utils.deployWebProjectUtils;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.rich.app.utils.deployWebProjectUtils.ExecuteSysCommandUtil.executeNpmCommand;

/**
 * 打包构建 web 工程项目执行器
 *
 * @author DuRuiChi
 * @create 2025/12/29
 **/
@Component
@Slf4j
public class BuildWebProjectExecutor {
    /**
     * 默认的 dist 目录名称
     */
    private static final String DIST_DIR_NAME = "dist";

    /**
     * 最大重试次数
     */
    private static final int MAX_RETRIES = 3;

    /**
     * 异步打包构建项目（Npm 包管理器下的前端 Web 工程项目）
     *
     * @param projectPath 项目路径
     */
    public boolean buildProjectAsync(String projectPath) {
        // 使用原子布尔保证线程安全的状态判断
        AtomicBoolean isBuild = new AtomicBoolean(true);

        // 开启 JDK-21 的虚拟线程，异步执行构建任务，避免阻塞主流程
        Thread.ofVirtual()
                .name("vue-builder-" + System.currentTimeMillis())  // 设置线程名称
                .start(() -> {
                    try {
                        // 执行项目构建并设置结果
                        isBuild.set(buildProject(projectPath));
                    } catch (Exception e) {
                        // 捕获异常，设置构建失败状态
                        isBuild.set(false);
                        log.error("异步打包构建项目失败，错误: {}", e.getMessage(), e);
                    }
                });
        // 返回初始状态（注：由于是异步执行，此时可能还未完成）
        return isBuild.get();
    }

    /**
     * 打包构建项目 （Npm 包管理器下的前端 Web 工程项目）
     *
     * @param projectPath 项目路径
     * @return boolean
     **/
    public boolean buildProject(String projectPath) {
        // 步骤1：验证项目路径和基本文件结构
        File projectDir = validateProject(projectPath);
        if (projectDir == null) {
            return false;  // 验证失败，直接返回
        }

        log.info("开始打包构建 Vue 项目: {}", projectPath);

        // 步骤2：确保 package.json 中配置了 ESM 模块类型
        ensureEsmConfig(projectDir);

        // 步骤3：执行 npm install 安装依赖（最多重试 3 次）
        if (!executeNpmCommand(projectDir, "install", MAX_RETRIES)) {
            log.error("依赖安装失败，已重试 {} 次", MAX_RETRIES);
            return false;
        }

        // 步骤4：执行 npm run build 构建项目（最多重试 3 次）
        if (!executeNpmCommand(projectDir, "run build", MAX_RETRIES)) {
            log.error("项目打包构建失败，已重试 {} 次", MAX_RETRIES);
            return false;
        }

        // 步骤5：验证构建输出目录是否生成
        return verifyDistDirectory(projectDir);
    }

    /**
     * 校验打包构建前项目文件的合法性
     *
     * @param projectPath 项目路径
     * @return java.io.File
     **/
    private File validateProject(String projectPath) {
        // 校验项目路径是否为空
        if (projectPath == null || projectPath.trim().isEmpty()) {
            log.error("项目路径不能为空");
            return null;
        }

        // 校验项目目录是否存在
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("项目目录不存在: {}", projectPath);
            return null;
        }

        // 校验 package.json 文件是否存在（必须文件）
        File packageJson = new File(projectDir, "package.json");
        if (!packageJson.exists()) {
            log.error("package.json 文件不存在: {}", packageJson.getAbsolutePath());
            return null;
        }

        return projectDir;
    }

    /**
     * 校验打包构建后项目文件的合法性
     *
     * @param projectDir 项目路径
     * @return boolean
     **/
    private boolean verifyDistDirectory(File projectDir) {
        // 检查默认的 dist 目录是否生成
        File distDir = new File(projectDir, DIST_DIR_NAME);
        if (distDir.exists()) {
            log.info("项目打包构建成功，dist 目录: {}", distDir.getAbsolutePath());
            return true;
        }

        // 如果 dist 目录不存在，尝试查找其他常见的构建输出目录
        String[] possibleDirs = {"build", "output", "public"};
        for (String dir : possibleDirs) {
            if (new File(projectDir, dir).exists()) {
                log.warn("打包构建目录存在但名称不是 'dist'，实际目录: {}", dir);
                return true;
            }
        }

        // 所有可能的构建目录都不存在
        log.error("打包构建目录未生成，请检查构建配置和输出设置");
        return false;
    }

    private void ensureEsmConfig(File projectDir) {
        try {
            // 读取 package.json 文件
            File pkg = new File(projectDir, "package.json");
            if (!pkg.exists()) {
                return;  // 文件不存在，直接返回
            }

            // 读取文件内容并解析为 JSON
            String text = Files.readString(pkg.toPath(), StandardCharsets.UTF_8);
            JSONObject obj = JSONUtil.parseObj(text);

            // 检查 type 字段是否为 "module"
            String type = obj.getStr("type");
            if (type == null || !"module".equalsIgnoreCase(type)) {
                // 如果不是 "module"，设置为 "module"（启用 ESM 模块系统）
                obj.set("type", "module");
                String updated = obj.toStringPretty();
                Files.writeString(pkg.toPath(), updated, StandardCharsets.UTF_8);
                log.info("已设置 package.json 的 type 为 module（启用 ESM 模块）");
            }
        } catch (Exception e) {
            // ESM 配置失败不影响构建流程，只记录警告
            log.warn("ESM 配置处理失败: {}", e.getMessage());
        }
    }
}