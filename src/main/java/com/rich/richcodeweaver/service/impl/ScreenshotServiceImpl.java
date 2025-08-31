package com.rich.richcodeweaver.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.FileStorageService;
import com.rich.richcodeweaver.exception.ErrorCode;
import com.rich.richcodeweaver.exception.ThrowUtils;
import com.rich.richcodeweaver.service.ScreenshotService;
import com.rich.richcodeweaver.utils.WebScreenshotUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


/**
 * Web 网页截图服务实现类
 *
 * @author DuRuiChi
 * @create 2025/8/31
 **/
@Service
@Slf4j
public class ScreenshotServiceImpl implements ScreenshotService {

    /**
     * X-File-Storage 文件存储服务实例
     */
    @Resource
    private FileStorageService fileStorageService;

    /**
     * 生成并上传网页截图到 OSS
     *
     * @param webUrl 目标网页URL
     * @return String 截图在对象存储的访问URL，失败返回 null
     */
    @Override
    public String generateAndUploadScreenshot(String webUrl) {
        // 参数校验：如果URL为空则抛出参数错误异常
        ThrowUtils.throwIf(StrUtil.isBlank(webUrl), ErrorCode.PARAMS_ERROR, "网页URL不能为空");
        log.info("开始生成网页截图，URL: {}", webUrl);

        // 1. 调用工具类生成本地截图文件
        String localScreenshotPath = WebScreenshotUtils.saveWebPageScreenshot(webUrl);
        // 校验截图是否生成成功
        ThrowUtils.throwIf(StrUtil.isBlank(localScreenshotPath), ErrorCode.OPERATION_ERROR, "本地截图生成失败");

        try {
            // 2. 上传截图到对象存储
            String cosUrl = uploadScreenshotToCos(localScreenshotPath);
            // 校验上传是否成功
            ThrowUtils.throwIf(StrUtil.isBlank(cosUrl), ErrorCode.OPERATION_ERROR, "截图上传对象存储失败");

            log.info("网页截图生成并上传成功: {} -> {}", webUrl, cosUrl);
            return cosUrl;
        } finally {
            // 3. 无论上传成功与否，都清理本地临时文件
            cleanupLocalFile(localScreenshotPath);
        }
    }

    /**
     * 上传截图文件到对象存储（OSS）
     *
     * @param localScreenshotPath 本地截图文件路径
     * @return String 对象存储访问URL，失败返回null
     */
    private String uploadScreenshotToCos(String localScreenshotPath) {
        // 参数校验
        if (StrUtil.isBlank(localScreenshotPath)) {
            return null;
        }

        // 创建文件对象并检查文件是否存在
        File screenshotFile = new File(localScreenshotPath);
        if (!screenshotFile.exists()) {
            log.error("截图文件不存在: {}", localScreenshotPath);
            return null;
        }

        // 使用 X-File-Storage 上传文件到 OSS
        FileInfo fileInfo = fileStorageService.of(screenshotFile)
                // 设置存储路径
                .setPath(setOSSStoragePath(screenshotFile.getName()))
                // 执行上传操作
                .upload();

        // 返回上传结果：成功返回文件URL，失败返回错误信息
        return fileInfo == null ? "上传失败,请检查 OSS 配置！" : fileInfo.getUrl();
    }

    /**
     * 设置在 OSS 中的存储路径
     * 格式：/screenshots/年/月/日/文件名.jpg
     *
     * @param fileName 文件名
     * @return String 完整的对象存储键
     */
    private String setOSSStoragePath(String fileName) {
        // 获取当前日期并格式化为"年/月/日"格式
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        // 拼接完整路径
        return String.format("/screenshots/%s/%s", datePath, fileName);
    }

    /**
     * 清理本地临时文件
     * 删除截图文件及其所在目录
     *
     * @param localFilePath 本地文件路径
     */
    private void cleanupLocalFile(String localFilePath) {
        File localFile = new File(localFilePath);
        if (localFile.exists()) {
            // 获取文件所在目录
            File parentDir = localFile.getParentFile();
            // 删除整个目录（包含截图文件）
            FileUtil.del(parentDir);
            log.info("本地截图文件已清理: {}", localFilePath);
        }
    }
}