package com.rich.ai.aiTools.fileOperate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 项目文件路径解析器测试
 *
 * @author DuRuiChi
 * @create 2026/11/6
 **/
class ProjectPathResolverTest {

    private static final Long APP_ID = 90001L;

    @AfterEach
    void tearDown() {
        CreatAndWriteAiTool.APP_CODE_GEN_TYPE_CACHE.remove(APP_ID);
    }

    @Test
    void shouldResolveRelativePathInsideProject() {
        CreatAndWriteAiTool.APP_CODE_GEN_TYPE_CACHE.put(APP_ID, "vue_project");

        Path resolved = ProjectPathResolver.resolve(APP_ID, "src/App.vue");

        assertTrue(resolved.toString().replace('\\', '/').endsWith("vue_project_" + APP_ID + "/src/App.vue"));
    }

    @Test
    void shouldRejectParentTraversal() {
        CreatAndWriteAiTool.APP_CODE_GEN_TYPE_CACHE.put(APP_ID, "vue_project");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ProjectPathResolver.resolve(APP_ID, "../outside.txt"));

        assertTrue(exception.getMessage().contains("路径越界"));
    }

    @Test
    void shouldRejectAbsolutePath() {
        CreatAndWriteAiTool.APP_CODE_GEN_TYPE_CACHE.put(APP_ID, "vue_project");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ProjectPathResolver.resolve(APP_ID, "C:\\Windows\\win.ini"));

        assertTrue(exception.getMessage().contains("绝对路径"));
    }
}
