package com.rich.common.utils.deployWebProjectUtils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 项目构建执行器测试
 *
 * @author DuRuiChi
 * @create 2026/11/6
 **/
class BuildWebProjectExecutorTest {

    @TempDir
    Path tempDir;

    private final BuildWebProjectExecutor buildExecutor = new BuildWebProjectExecutor();

    @Test
    void shouldFailWhenPackageJsonIsMissing() {
        BuildWebProjectExecutor.BuildResult result =
                buildExecutor.buildProjectWithLog(tempDir.toString());

        assertFalse(result.success());
        assertTrue(result.phase().contains("validate"));
    }

    @Test
    void asyncBuildShouldReturnRealFailureResult() throws Exception {
        BuildWebProjectExecutor.BuildResult result =
                buildExecutor.buildProjectAsync(tempDir.toString()).get(5, TimeUnit.SECONDS);

        assertFalse(result.success());
    }
}
