package com.rich.common.utils.deployWebProjectUtils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 打包构建 web 工程项目执行器
 * 统一负责依赖安装、Vite 构建、dist 校验和并发互斥，避免工作流、Agent 与消息队列重复构建
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
     * 默认依赖安装超时时间（秒）
     */
    private static final int INSTALL_TIMEOUT_SECONDS = 600;

    /**
     * 默认构建超时时间（秒）
     */
    private static final int BUILD_TIMEOUT_SECONDS = 300;

    /**
     * 同一项目构建锁，key 为项目绝对路径
     */
    private static final ConcurrentHashMap<String, ReentrantLock> PROJECT_BUILD_LOCKS = new ConcurrentHashMap<>();

    /**
     * 异步构建项目并返回可等待的真实结果
     *
     * @param projectPath 项目路径
     * @return 异步构建结果
     */
    public CompletableFuture<BuildResult> buildProjectAsync(String projectPath) {
        CompletableFuture<BuildResult> future = new CompletableFuture<>();
        Thread.ofVirtual()
                .name("vue-builder-" + System.currentTimeMillis())
                .start(() -> {
                    try {
                        future.complete(buildProjectWithLog(projectPath));
                    } catch (Exception e) {
                        future.complete(new BuildResult(false, "异步构建", e.getMessage()));
                        log.error("异步打包构建项目失败，错误: {}", e.getMessage(), e);
                    }
                });
        return future;
    }

    /**
     * 同步构建项目并返回详细结果日志（供工作流、Agent 和消息队列使用）
     *
     * @param projectPath 项目路径
     * @return BuildResult 包含是否成功、阶段信息和详细日志
     */
    public BuildResult buildProjectWithLog(String projectPath) {
        File projectDir = validateProject(projectPath);
        if (projectDir == null) {
            return new BuildResult(false, "validate", "项目目录不存在或缺少 package.json: " + projectPath);
        }

        ReentrantLock buildLock = PROJECT_BUILD_LOCKS.computeIfAbsent(
                projectDir.getAbsolutePath(), key -> new ReentrantLock());
        boolean locked = false;
        try {
            locked = buildLock.tryLock(10, TimeUnit.MINUTES);
            if (!locked) {
                return new BuildResult(false, "build-lock", "同一项目已有构建任务执行超过等待时间");
            }
            return doBuildProjectWithLog(projectDir);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new BuildResult(false, "build-lock", "等待项目构建锁时被中断");
        } finally {
            if (locked) {
                buildLock.unlock();
            }
            if (!buildLock.hasQueuedThreads()) {
                PROJECT_BUILD_LOCKS.remove(projectDir.getAbsolutePath(), buildLock);
            }
        }
    }

    /**
     * 执行实际构建流程
     *
     * @param projectDir 项目目录
     * @return 构建结果
     */
    private BuildResult doBuildProjectWithLog(File projectDir) {
        log.info("开始带日志的构建，项目路径: {}", projectDir.getAbsolutePath());
        ensureEsmConfig(projectDir);

        File distDir = new File(projectDir, DIST_DIR_NAME);
        if (distDir.exists() && !FileUtil.del(distDir)) {
            return new BuildResult(false, "clean-dist", "无法清理旧 dist 目录: " + distDir.getAbsolutePath());
        }

        String npmCommand = getNpmCommand();
        File packageJson = new File(projectDir, "package.json");
        File packageLock = new File(projectDir, "package-lock.json");
        boolean lockFileFresh = packageLock.isFile()
                && packageLock.lastModified() >= packageJson.lastModified();
        String installCommand = lockFileFresh
                ? npmCommand + " ci --no-audit --no-fund"
                : npmCommand + " install --no-audit --no-fund";
        ExecuteSysCommandUtil.CommandResult installResult =
                ExecuteSysCommandUtil.executeCommandWithLog(projectDir, installCommand, INSTALL_TIMEOUT_SECONDS);
        if (!installResult.success()) {
            return new BuildResult(false, "npm install", installResult.log());
        }

        String buildCommand = npmCommand + " run build";
        ExecuteSysCommandUtil.CommandResult buildResult =
                ExecuteSysCommandUtil.executeCommandWithLog(projectDir, buildCommand, BUILD_TIMEOUT_SECONDS);
        if (!buildResult.success()) {
            return new BuildResult(false, "npm run build", buildResult.log());
        }

        File indexHtml = new File(distDir, "index.html");
        if (!indexHtml.isFile() || indexHtml.length() == 0) {
            return new BuildResult(false, "dist验证",
                    "构建命令执行成功但未找到非空 dist/index.html，请检查 vite.config 的 outDir 配置");
        }
        return new BuildResult(true, "完成", "构建成功，dist/index.html 已生成: " + indexHtml.getAbsolutePath());
    }

    /**
     * 构建结果封装
     *
     * @param success 是否成功
     * @param phase   失败阶段
     * @param log     构建日志
     */
    public record BuildResult(boolean success, String phase, String log) {
    }

    /**
     * 打包构建项目
     *
     * @param projectPath 项目路径
     * @return 是否成功
     **/
    public boolean buildProject(String projectPath) {
        return buildProjectWithLog(projectPath).success();
    }

    /**
     * 校验打包构建前项目文件的合法性
     *
     * @param projectPath 项目路径
     * @return 项目目录，非法时返回 null
     **/
    private File validateProject(String projectPath) {
        if (StrUtil.isBlank(projectPath)) {
            log.error("项目路径不能为空");
            return null;
        }

        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("项目目录不存在: {}", projectPath);
            return null;
        }

        File packageJson = new File(projectDir, "package.json");
        if (!packageJson.isFile()) {
            log.error("package.json 文件不存在: {}", packageJson.getAbsolutePath());
            return null;
        }
        return projectDir;
    }

    /**
     * 确保 package.json 使用 ESM 模块类型
     *
     * @param projectDir 项目目录
     */
    private void ensureEsmConfig(File projectDir) {
        try {
            File packageJson = new File(projectDir, "package.json");
            if (!packageJson.isFile()) {
                return;
            }
            String text = Files.readString(packageJson.toPath(), StandardCharsets.UTF_8);
            JSONObject packageObject = JSONUtil.parseObj(text);
            String type = packageObject.getStr("type");
            if (!"module".equalsIgnoreCase(type)) {
                packageObject.set("type", "module");
                Files.writeString(packageJson.toPath(), packageObject.toStringPretty(), StandardCharsets.UTF_8);
                log.info("已设置 package.json 的 type 为 module（启用 ESM 模块）");
            }
        } catch (Exception e) {
            log.warn("ESM 配置处理失败: {}", e.getMessage());
        }
    }

    /**
     * 根据平台系统拼装合法的 npm 命令
     *
     * @return npm 命令
     **/
    private String getNpmCommand() {
        return System.getProperty("os.name").toLowerCase().contains("win") ? "npm.cmd" : "npm";
    }
}
