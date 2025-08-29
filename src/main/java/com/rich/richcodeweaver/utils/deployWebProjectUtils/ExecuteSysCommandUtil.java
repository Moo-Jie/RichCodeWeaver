package com.rich.richcodeweaver.utils.deployWebProjectUtils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 执行系统命令工具
 *
 * @author DuRuiChi
 * @create 2025/8/29
 **/
@Slf4j
public class ExecuteSysCommandUtil {
    /**
     * 默认超时时间
     */
    private static final int DEFAULT_TIMEOUT = 300;

    /**
     * npm命令
     */
    private static final String NPM_CMD = "npm";

    /**
     * 执行 Npm 命令
     *
     * @param projectDir 项目目录
     * @param command    命令
     * @param maxRetries 最大重试次数
     * @return boolean
     **/
    public static boolean executeNpmCommand(File projectDir, String command, int maxRetries) {
        // 根据平台系统拼装合法的 npm 命令
        String fullCommand = getNpmCommand() + " " + command;
        log.info("执行命令: {} (超时: {}秒)", fullCommand, DEFAULT_TIMEOUT);

        // 尝试执行命令
        int attempt = 0;
        while (attempt < maxRetries) {
            if (executeCommand(projectDir, fullCommand, DEFAULT_TIMEOUT)) {
                return true;
            }
            // 如果命令执行失败，则进行重试
            attempt++;
            if (attempt < maxRetries) {
                log.warn("命令执行失败, 重试中 ({}/{})", attempt, maxRetries);
            }
        }
        log.error("命令执行失败: {}", fullCommand);
        return false;
    }

    /**
     * 执行命令
     *
     * @param projectDir     工作目录
     * @param command        命令
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否执行成功
     */
    public static boolean executeCommand(File projectDir, String command, int timeoutSeconds) {
        log.info("在目录 {} 中执行命令: {}", projectDir.getAbsolutePath(), command);

        Process process = null;
        try {
            // 使用更健壮的命令行解析方式
            String[] parsedCommand = parseCommand(command);
            process = RuntimeUtil.exec(null, projectDir, parsedCommand);

            // 使用 CompletableFuture 异步读取输出
            CompletableFuture<String> outputFuture = readStream(process.getInputStream());
            CompletableFuture<String> errorFuture = readStream(process.getErrorStream());

            // 等待进程完成或超时
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                log.error("命令执行超时（{}秒），强制终止进程", timeoutSeconds);
                process.destroyForcibly();
                return false;
            }

            // 获取命令输出
            String output = outputFuture.get(1, TimeUnit.SECONDS);
            String error = errorFuture.get(1, TimeUnit.SECONDS);

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("命令执行成功: {}", command);
                if (StrUtil.isNotBlank(output)) {
                    log.debug("标准输出:\n{}", truncateLongText(output));
                }
                return true;
            } else {
                log.error("命令执行失败，退出码: {}\n命令: {}\n错误输出:\n{}",
                        exitCode, command, truncateLongText(error));
                return false;
            }
        } catch (TimeoutException e) {
            log.error("读取命令输出超时: {}", command);
            return false;
        } catch (Exception e) {
            log.error("执行命令失败: {}, 错误信息: {}", command, e.getMessage());
            return false;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * 命令解析，处理带空格的参数
     */
    private static String[] parseCommand(String command) {
        List<String> commands = new ArrayList<>();
        boolean inQuote = false;
        StringBuilder current = new StringBuilder();

        for (char c : command.toCharArray()) {
            if (c == '"') {
                inQuote = !inQuote;
            } else if (Character.isWhitespace(c) && !inQuote) {
                if (current.length() > 0) {
                    commands.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            commands.add(current.toString());
        }

        return commands.toArray(new String[0]);
    }

    /**
     * 异步读取流内容
     */
    private static CompletableFuture<String> readStream(InputStream inputStream) {
        return CompletableFuture.supplyAsync(() -> {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append(System.lineSeparator());
                }
            } catch (Exception e) {
                log.warn("读取命令输出失败: {}", e.getMessage());
            }
            return content.toString();
        });
    }

    /**
     * 截断过长的文本
     */
    private static String truncateLongText(String text) {
        final int MAX_LENGTH = 2000;
        if (text.length() > MAX_LENGTH) {
            return text.substring(0, MAX_LENGTH) + "\n... [输出被截断，总长度: " + text.length() + " 字符]";
        }
        return text;
    }

    /**
     * 根据平台系统拼装合法的 npm 命令
     *
     * @return java.lang.String
     **/
    private static String getNpmCommand() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("win") ? NPM_CMD + ".cmd" : NPM_CMD;
    }
}