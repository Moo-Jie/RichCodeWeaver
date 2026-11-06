package com.rich.app.quality;

import com.rich.model.enums.CodeGeneratorTypeEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 产物质量门禁测试
 *
 * @author DuRuiChi
 * @create 2026/11/6
 **/
class ProjectQualityGateServiceTest {

    @TempDir
    Path tempDir;

    private final ProjectQualityGateService qualityGateService = new ProjectQualityGateService();

    @Test
    void shouldPassValidVueProject() throws Exception {
        createVueProject();

        ProjectQualityGateService.ValidationResult result =
                qualityGateService.validate(tempDir.toString(), CodeGeneratorTypeEnum.VUE_PROJECT);

        assertTrue(result.passed(), () -> String.join(";", result.errors()));
    }

    @Test
    void shouldRejectMissingImportedComponent() throws Exception {
        createVueProject();
        Files.writeString(tempDir.resolve("src/App.vue"), """
                <script setup>
                import Missing from './components/Missing.vue'
                </script>
                <template><Missing /></template>
                """);

        ProjectQualityGateService.ValidationResult result =
                qualityGateService.validate(tempDir.toString(), CodeGeneratorTypeEnum.VUE_PROJECT);

        assertFalse(result.passed());
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("Missing.vue")));
    }

    @Test
    void shouldRejectPackageWithoutBuildScript() throws Exception {
        createVueProject();
        Files.writeString(tempDir.resolve("package.json"), """
                {
                  "dependencies": {"vue": "^3.5.0"},
                  "devDependencies": {"vite": "^7.0.0", "@vitejs/plugin-vue": "^6.0.0"}
                }
                """);

        ProjectQualityGateService.ValidationResult result =
                qualityGateService.validate(tempDir.toString(), CodeGeneratorTypeEnum.VUE_PROJECT);

        assertFalse(result.passed());
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("scripts.build")));
    }

    private void createVueProject() throws Exception {
        Files.createDirectories(tempDir.resolve("src/router"));
        Files.createDirectories(tempDir.resolve("src/components"));
        Files.writeString(tempDir.resolve("package.json"), """
                {
                  "scripts": {"build": "vite build"},
                  "dependencies": {"vue": "^3.5.0", "vue-router": "^4.5.0"},
                  "devDependencies": {"vite": "^7.0.0", "@vitejs/plugin-vue": "^6.0.0"}
                }
                """);
        Files.writeString(tempDir.resolve("vite.config.js"), """
                import { defineConfig } from 'vite'
                import vue from '@vitejs/plugin-vue'
                export default defineConfig({ base: './', plugins: [vue()] })
                """);
        Files.writeString(tempDir.resolve("index.html"), """
                <!doctype html>
                <html><body><div id="app"></div><script type="module" src="/src/main.js"></script></body></html>
                """);
        Files.writeString(tempDir.resolve("src/main.js"), """
                import { createApp } from 'vue'
                import App from './App.vue'
                import router from './router/index.js'
                createApp(App).use(router).mount('#app')
                """);
        Files.writeString(tempDir.resolve("src/App.vue"), """
                <script setup>
                import Hello from './components/Hello.vue'
                </script>
                <template><Hello /></template>
                """);
        Files.writeString(tempDir.resolve("src/components/Hello.vue"), "<template><main>Hello</main></template>");
        Files.writeString(tempDir.resolve("src/router/index.js"), """
                import { createRouter, createWebHashHistory } from 'vue-router'
                export default createRouter({ history: createWebHashHistory(), routes: [] })
                """);
    }
}
