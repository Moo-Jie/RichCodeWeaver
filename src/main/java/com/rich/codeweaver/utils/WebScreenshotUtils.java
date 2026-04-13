package com.rich.codeweaver.utils;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.ScreenshotType;
import com.microsoft.playwright.options.WaitUntilState;
import com.rich.codeweaver.common.constant.AppConstant;
import com.rich.codeweaver.common.exception.BusinessException;
import com.rich.codeweaver.common.exception.ErrorCode;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Web 页面截图工具类 - 基于 Playwright 实现
 * 跨平台稳定方案，支持 Windows/Linux/macOS
 * 自带浏览器，无需安装 Chrome，零配置开箱即用
 *
 * @author DuRuiChi
 * @create 2025/12/31
 **/
@Slf4j
@Component
public class WebScreenshotUtils {
    // 双重检查锁确保线程安全的单例初始化
    private static final Object LOCK = new Object();
    // Playwright 实例（单例，线程安全）
    private static volatile Playwright playwright;
    // Browser 实例（复用，提升性能）
    private static volatile Browser browser;

    /**
     * 获取 Playwright 实例（懒加载 + 双重检查锁）
     *
     * @return Playwright 实例
     */
    private static Playwright getPlaywright() {
        if (playwright == null) {
            synchronized (LOCK) {
                if (playwright == null) {
                    log.info("初始化 Playwright 实例...");
                    playwright = Playwright.create();
                    log.info("Playwright 初始化成功");
                }
            }
        }
        return playwright;
    }

    /**
     * 获取 Browser 实例（懒加载 + 双重检查锁）
     * 复用 Browser 实例提升性能
     *
     * @return Browser 实例
     */
    private static Browser getBrowser() {
        if (browser == null || !browser.isConnected()) {
            synchronized (LOCK) {
                if (browser == null || !browser.isConnected()) {
                    log.info("初始化 Chromium 浏览器...");
                    Playwright pw = getPlaywright();
                    browser = pw.chromium().launch(new BrowserType.LaunchOptions()
                            .setHeadless(true)  // 无头模式
                            .setArgs(java.util.Arrays.asList(
                                    "--no-sandbox",
                                    "--disable-dev-shm-usage",
                                    "--disable-gpu"
                            ))
                    );
                    log.info("Chromium 浏览器初始化成功");
                }
            }
        }
        return browser;
    }

    /**
     * 生成网页截图并保存到本地
     *
     * @param webUrl 要截图的网页URL
     * @return String 压缩后的图片保存路径
     */
    public static String saveWebPageScreenshot(String webUrl) {
        // 非空校验
        if (StrUtil.isBlank(webUrl)) {
            log.error("网页截图失败，url为空");
            return null;
        }

        Page page = null;
        try {
            // 创建临时目录，使用UUID前8位作为目录名的一部分
            String rootPath = AppConstant.APP_SCREENSHOT_DIR + File.separator + UUID.randomUUID().toString().substring(0, 8);
            FileUtil.mkdir(rootPath);

            // 图片后缀
            final String IMAGE_SUFFIX = ".png";
            // 原始图片保存路径，使用5位随机数作为文件名
            String imageSavePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + IMAGE_SUFFIX;

            // 获取 Browser 实例并创建新页面
            Browser browserInstance = getBrowser();
            page = browserInstance.newPage(new Browser.NewPageOptions()
                    .setViewportSize(1600, 900)  // 设置视口大小
            );

            // 访问网页（Playwright 自动等待页面加载）
            log.info("开始访问网页：{}", webUrl);
            page.navigate(webUrl, new Page.NavigateOptions()
                    .setTimeout(30000)  // 30秒超时
                    .setWaitUntil(WaitUntilState.NETWORKIDLE)  // 等待网络空闲
            );

            // 额外等待确保动态内容加载完成
            page.waitForTimeout(3000);
            log.info("页面加载完成");

            // 截图并保存（Playwright 直接保存到文件）
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(imageSavePath))
                    .setType(ScreenshotType.PNG)
                    .setFullPage(false)  // 只截取视口内容
            );
            log.info("原始截图保存成功：{}", imageSavePath);

            // 压缩图片
            final String COMPRESS_SUFFIX = "_compressed.jpg";
            String compressedImagePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + COMPRESS_SUFFIX;
            compressImage(imageSavePath, compressedImagePath);
            log.info("压缩图片保存成功：{}", compressedImagePath);

            // 删除原始图片以节省空间
            FileUtil.del(imageSavePath);

            return compressedImagePath;
        } catch (Exception e) {
            log.error("网页截图失败：{}", webUrl, e);
            return null;
        } finally {
            // 关闭页面释放资源（但保留 Browser 实例复用）
            if (page != null) {
                try {
                    page.close();
                } catch (Exception e) {
                    log.warn("关闭页面失败", e);
                }
            }
        }
    }

    /**
     * 压缩图片
     *
     * @param originImagePath     原始图片路径
     * @param compressedImagePath 压缩后图片保存路径
     */
    private static void compressImage(String originImagePath, String compressedImagePath) {
        // 压缩质量设置为30%
        final float COMPRESSION_QUALITY = 0.3f;
        try {
            // 使用Hutool图片压缩工具
            ImgUtil.compress(
                    // 原始图片文件
                    FileUtil.file(originImagePath),
                    // 压缩后图片文件
                    FileUtil.file(compressedImagePath),
                    // 压缩质量
                    COMPRESSION_QUALITY
            );
        } catch (Exception e) {
            log.error("压缩图片失败：{} -> {}", originImagePath, compressedImagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "压缩图片失败");
        }
    }

    /**
     * 销毁方法，在Spring容器关闭时自动调用
     * 关闭浏览器和 Playwright 实例释放资源
     */
    @PreDestroy
    public void destroy() {
        synchronized (LOCK) {
            // 关闭 Browser 实例
            if (browser != null) {
                try {
                    browser.close();
                    log.info("Browser 实例已关闭");
                } catch (Exception e) {
                    log.error("关闭 Browser 实例失败", e);
                } finally {
                    browser = null;
                }
            }
            // 关闭 Playwright 实例
            if (playwright != null) {
                try {
                    playwright.close();
                    log.info("Playwright 实例已关闭");
                } catch (Exception e) {
                    log.error("关闭 Playwright 实例失败", e);
                } finally {
                    playwright = null;
                }
            }
        }
    }
}