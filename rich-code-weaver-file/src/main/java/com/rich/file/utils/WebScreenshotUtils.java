package com.rich.file.utils;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.rich.common.constant.AppConstant;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Web 页面截图工具类，包含网页截图、保存和压缩等逻辑
 *
 * @author DuRuiChi
 * @create 2025/8/31
 **/
@Slf4j
public class WebScreenshotUtils {
    // WebDriver 实例线程池,防止多线程并发时，截图被后生成的截图覆盖
    // WebDriver 实例，一个应用使用一个浏览器实例，用于执行浏览器操作
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    // 用于跟踪所有 WebDriver 实例的集合，保证线程安全
    private static final Set<WebDriver> allDrivers = Collections.synchronizedSet(new HashSet<>());

    // 静态初始化块，在类加载时初始化 WebDriver 实例及其配置信息，否则容易初始化 Chrome 浏览器失败
    static {
        // 静态设置 ChromeDriver 下载镜像源（ /chrome-for-testing 下的镜像源版本比较全）
        System.setProperty("wdm.chromeDownloadUrl", "https://registry.npmmirror.com/binary.html?path=chrome-for-testing/");
        // 静态设置 WebDriverManager 超时时间为300   秒
        System.setProperty("wdm.timeout", "300");
        // 静态设置 WebDriverManager 重试次数为3次
        System.setProperty("wdm.retryCount", "3");
    }

    /**
     * 从线程池获取 WebDriver 实例
     *
     * @return org.openqa.selenium.WebDriver
     * @author DuRuiChi
     * @create 2025/9/3
     **/
    public static WebDriver getDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver == null) {
            driver = initChromeDriver();
            driverThreadLocal.set(driver);
            // 将新实例添加到全局集合
            allDrivers.add(driver);
        }
        // 检查驱动是否有效（例如会话是否存活）
        try {
            // 检查驱动是否有效
            driver.getWindowHandle();
        } catch (Exception e) {
            log.warn("WebDriver实例无效，重新初始化");
            // 关闭无效实例
            driver.quit();
            // 从集合移除
            allDrivers.remove(driver);
            // 清理ThreadLocal
            driverThreadLocal.remove();
            // 重新初始化
            driver = initChromeDriver();
            driverThreadLocal.set(driver);
            allDrivers.add(driver);
        }
        return driver;
    }

    /**
     * 生成网页截图并保存到本地
     *
     * @param webUrl 要截图的网页URL
     * @return String 压缩后的图片保存路径
     **/
    public static String saveWebPageScreenshot(String webUrl) {
        // 非空校验
        if (StrUtil.isBlank(webUrl)) {
            log.error("网页截图失败，url为空");
            return null;
        }

        try {
            // 创建临时目录，使用UUID前8位作为目录名的一部分
            String rootPath = AppConstant.APP_SCREENSHOT_DIR + File.separator + UUID.randomUUID().toString().substring(0, 8);
            FileUtil.mkdir(rootPath);  // 创建目录

            // 图片后缀
            final String IMAGE_SUFFIX = ".png";
            // 原始图片保存路径，使用5位随机数作为文件名
            String imageSavePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + IMAGE_SUFFIX;

            // 从线程池获取 WebDriver 实例
            WebDriver webDriver = getDriver();
            // 访问网页
            webDriver.get(webUrl);
            // 等待网页加载完成
            waitForPageLoad();

            // 截图并获取字节数组
            byte[] screenshotBytes = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);
            // 保存原始图片
            saveImage(screenshotBytes, imageSavePath);
            log.info("原始截图保存成功：{}", imageSavePath);

            // 压缩图片
            final String COMPRESS_SUFFIX = "_compressed.jpg";
            // 压缩图片保存路径，使用5位随机数作为文件名
            String compressedImagePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + COMPRESS_SUFFIX;
            // 压缩图片
            compressImage(imageSavePath, compressedImagePath);
            log.info("压缩图片保存成功：{}", compressedImagePath);

            // 删除原始图片以节省空间
            FileUtil.del(imageSavePath);

            // 返回压缩后的图片路径
            return compressedImagePath;
        } catch (Exception e) {
            log.error("网页截图失败：{}", webUrl, e);
            return null;
        }
    }

    /**
     * 初始化Chrome浏览器驱动
     *
     * @return WebDriver 初始化后的WebDriver实例
     */
    private static WebDriver initChromeDriver() {
        try {
            // 使用WebDriverManager自动管理ChromeDriver
            WebDriverManager.chromedriver()
                    // 固定 ChromeDriver 版本 （自动指定版本号会出现版本不一致、版本太老等问题）
                    // TODO 部署时服务器 Chrome 版本匹配
                    .driverVersion("139.0.7258.154")
                    // 使用镜像下载
                    .useMirror()
                    // 下载并设置驱动
                    .setup();

            // 获取配置好的WebDriver实例
            WebDriver driver = getWebDriver();
            // 设置页面加载超时为30秒
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            // 设置隐式等待时间为10秒
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            return driver;
        } catch (Exception e) {
            log.error("初始化 Chrome 浏览器失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化Chrome浏览器失败");
        }
    }

    /**
     * 配置并创建ChromeDriver实例
     *
     * @return WebDriver 配置好的ChromeDriver实例
     */
    private static WebDriver getWebDriver() {
        // 创建Chrome选项对象
        ChromeOptions options = new ChromeOptions();
        // 无头模式（不显示浏览器界面）
        options.addArguments("--headless");
        // 禁用GPU加速（避免某些环境下的兼容性问题）
        options.addArguments("--disable-gpu");
        // 禁用沙盒模式（Docker环境必需）
        options.addArguments("--no-sandbox");
        // 禁用开发者共享内存使用（Docker环境必需）
        options.addArguments("--disable-dev-shm-usage");
        // 设置浏览器窗口大小
        options.addArguments(String.format("--window-size=%d,%d", 1600, 900));
        // 禁用浏览器扩展
        options.addArguments("--disable-extensions");
        // 设置用户代理字符串
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

        // 创建ChromeDriver实例并传入配置选项
        WebDriver driver = new ChromeDriver(options);
        return driver;
    }

    /**
     * 保存图片到指定路径
     *
     * @param imageBytes 图片字节数组
     * @param imagePath  图片保存路径
     */
    private static void saveImage(byte[] imageBytes, String imagePath) {
        try {
            // 使用Hutool工具将字节数组写入文件
            FileUtil.writeBytes(imageBytes, imagePath);
        } catch (Exception e) {
            log.error("保存图片失败：{}", imagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存图片失败");
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
     * 等待页面加载完成
     */
    private static void waitForPageLoad() {
        try {
            // 从线程池获取当前线程的 WebDriver 实例（saveWebPageScreenshot 中使用过的）
            WebDriver webDriver = getDriver();
            // 创建WebDriverWait对象，设置超时时间为10秒
            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));
            // 等待直到页面文档加载完成（readyState 为 complete）
            wait.until(driver -> ((JavascriptExecutor) driver)
                    .executeScript("return document.readyState")
                    .equals("complete")
            );
            // 额外等待 3 秒，确保动态内容（如JavaScript、AJAX）加载完成
            log.info("等待页面加载...");
            Thread.sleep(3000);
            log.info("页面加载完成");
        } catch (Exception e) {
            // 记录异常但不中断执行，继续尝试截图
            log.error("等待页面加载时出现异常，继续执行截图", e);
        }
    }

    /**
     * 销毁方法，在Spring容器关闭时自动调用
     * 关闭浏览器释放资源
     */
    @PreDestroy
    public void destroy() {
        // 清理所有WebDriver实例
        synchronized (allDrivers) {
            for (WebDriver driver : allDrivers) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    log.error("关闭 WebDriver 实例失败", e);
                }
            }
            allDrivers.clear();
        }
        // 清理当前线程的ThreadLocal
        driverThreadLocal.remove();
    }
}