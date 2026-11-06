package com.rich.file.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.rich.common.constant.AppConstant;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.file.service.FileService;
import com.rich.file.service.ScreenshotService;
import com.rich.file.utils.WebScreenshotUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static com.rich.file.utils.MultipartFileUtils.getMultipartFile;


/**
 * Web 网页截图服务实现类
 * 实现网页截图生成和上传到OSS的具体逻辑
 *
 * @author DuRuiChi
 * @since 2026-03-11
 */
@Service
@Slf4j
public class ScreenshotServiceImpl implements ScreenshotService {
    @Resource
    private FileService fileService;

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
            // 2. 优先上传到对象存储
            String screenshotUrl = uploadScreenshotToOssQuietly(localScreenshotPath);

            // 3. 对象存储不可用时降级保存到 Nginx 静态封面目录，保证本地环境仍可更新封面
            if (StrUtil.isBlank(screenshotUrl)) {
                screenshotUrl = saveScreenshotToLocal(localScreenshotPath, webUrl);
            }

            ThrowUtils.throwIf(StrUtil.isBlank(screenshotUrl), ErrorCode.OPERATION_ERROR, "截图保存失败");
            log.info("网页截图生成并保存成功: {} -> {}", webUrl, screenshotUrl);
            return screenshotUrl;
        } finally {
            cleanupLocalFile(localScreenshotPath);
        }
    }

    /**
     * 上传截图到对象存储，失败时返回空值以便上层执行本地降级
     *
     * @param localScreenshotPath 本地截图路径
     * @return 对象存储访问 URL，失败时返回 null
     */
    private String uploadScreenshotToOssQuietly(String localScreenshotPath) {
        try {
            return uploadScreenshotToOss(localScreenshotPath);
        } catch (Exception e) {
            log.warn("截图上传对象存储失败，降级保存到本地封面目录: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 保存截图到本地静态封面目录
     *
     * @param localScreenshotPath 本地截图路径
     * @param webUrl              网页 URL
     * @return 本地封面访问 URL，失败时返回 null
     */
    private String saveScreenshotToLocal(String localScreenshotPath, String webUrl) {
        File sourceFile = new File(localScreenshotPath);
        if (!sourceFile.exists()) {
            log.error("本地截图文件不存在: {}", localScreenshotPath);
            return null;
        }

        String extension = FileUtil.extName(sourceFile);
        String fileName = resolveCoverFileName(webUrl, extension);
        File coverDir = FileUtil.mkdir(new File(AppConstant.CODE_DEPLOY_ROOT_DIR, "_covers"));
        File targetFile = new File(coverDir, fileName);
        FileUtil.copy(sourceFile, targetFile, true);
        return AppConstant.CODE_DEPLOY_HOST + "/_covers/" + fileName;
    }

    /**
     * 根据部署地址生成稳定的封面文件名
     *
     * @param webUrl    网页 URL
     * @param extension 图片扩展名
     * @return 封面文件名
     */
    private String resolveCoverFileName(String webUrl, String extension) {
        try {
            String path = URI.create(webUrl).getPath();
            String deployKey = StrUtil.subBefore(StrUtil.removePrefix(path, "/"), "/", false);
            if (StrUtil.isNotBlank(deployKey)) {
                return deployKey + "-cover." + extension;
            }
        } catch (Exception e) {
            log.warn("解析产物部署地址失败，使用随机封面文件名: {}", webUrl);
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8) + "-cover." + extension;
    }

    /**
     * 上传截图文件到对象存储（OSS）
     *
     * @param localScreenshotPath 本地截图文件路径
     * @return String 对象存储访问URL，失败返回null
     */
    private String uploadScreenshotToOss(String localScreenshotPath) {
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

        // 文件上传至 OSS
        String fileUrl = fileService.upload(getMultipartFile(screenshotFile));

        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.OPERATION_ERROR, "截图上传对象存储失败");

        // 返回上传 Url
        return fileUrl;
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