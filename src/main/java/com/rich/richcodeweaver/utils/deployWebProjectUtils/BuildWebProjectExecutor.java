package com.rich.richcodeweaver.utils.deployWebProjectUtils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.rich.richcodeweaver.utils.deployWebProjectUtils.ExecuteSysCommandUtil.executeNpmCommand;

/**
 * 打包构建 web 工程项目执行器
 *
 * @author DuRuiChi
 * @create 2025/8/29
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
        // 原子布尔保证正确判断
        AtomicBoolean isBuild = new AtomicBoolean(true);
        // 开启 JDK-21 的虚拟线程，避免阻塞主流程
        Thread.ofVirtual().name("vue-builder-" + System.currentTimeMillis()).start(() -> {
            try {
                isBuild.set(buildProject(projectPath));
            } catch (Exception e) {
                isBuild.set(false);
                log.error("异步打包构建项目失败，因为: {}", e.getMessage(), e);
            }
        });
        return isBuild.get();
    }

    /**
     * 打包构建项目 （Npm 包管理器下的前端 Web 工程项目）
     *
     * @param projectPath 项目路径
     * @return boolean
     **/
    public boolean buildProject(String projectPath) {
        // 验证项目路径
        File projectDir = validateProject(projectPath);
        if (projectDir == null) return false;

        log.info("开始打包构建 Vue 项目: {}", projectPath);

        // 反转布尔逻辑：成功时继续执行
        if (!executeNpmCommand(projectDir, "install", MAX_RETRIES)) {
            log.error("依赖安装失败");
            return false;
        }

        if (!executeNpmCommand(projectDir, "run build", MAX_RETRIES)) {
            log.error("项目打包构建失败");
            return false;
        }

        // 检查项目资源路径合法性
        return verifyDistDirectory(projectDir);
    }

    /**
     * 校验打包构建前项目文件的合法性
     *
     * @param projectPath 项目路径
     * @return java.io.File
     **/
    private File validateProject(String projectPath) {
        if (projectPath == null || projectPath.trim().isEmpty()) {
            log.error("项目路径不能为空");
            return null;
        }

        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("项目目录不存在: {}", projectPath);
            return null;
        }

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
        File distDir = new File(projectDir, DIST_DIR_NAME);
        if (distDir.exists()) {
            log.info("项目打包构建成功，dist目录: {}", distDir.getAbsolutePath());
            return true;
        }

        // 尝试常见打包构建目录名称
        String[] possibleDirs = {"build", "output", "public"};
        for (String dir : possibleDirs) {
            if (new File(projectDir, dir).exists()) {
                log.warn("打包构建目录存在但名称不是 'dist' : {}", dir);
                return true;
            }
        }

        log.error("打包构建目录未生成，请检查打包构建输出");
        return false;
    }
}