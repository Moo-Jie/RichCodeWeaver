package com.rich.app.utils.codeConcatenate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 代码审查输入拼接测试
 *
 * @author DuRuiChi
 * @create 2026/11/6
 **/
class CodeConcatenateUtilesTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldExcludeDependenciesBuildOutputsAndLockFiles() throws Exception {
        Files.createDirectories(tempDir.resolve("src"));
        Files.createDirectories(tempDir.resolve("node_modules/vite"));
        Files.createDirectories(tempDir.resolve("dist"));
        Files.writeString(tempDir.resolve("src/main.js"), "console.log('project-source-marker')");
        Files.writeString(tempDir.resolve("node_modules/vite/package.json"), "{\"marker\":\"dependency-marker\"}");
        Files.writeString(tempDir.resolve("dist/index.html"), "<html>dist-marker</html>");
        Files.writeString(tempDir.resolve("package-lock.json"), "{\"marker\":\"lock-marker\"}");

        String content = CodeConcatenateUtiles.readAndConcatenateCodeFiles(tempDir.toString());

        assertTrue(content.contains("project-source-marker"));
        assertFalse(content.contains("dependency-marker"));
        assertFalse(content.contains("dist-marker"));
        assertFalse(content.contains("lock-marker"));
    }
}
