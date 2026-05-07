package com.rich.file.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.file.config.OSSConfig;
import com.rich.file.mq.producer.FileUploadProducer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 文件上传服务异步实现
 * 基于消息队列的异步上传
 *
 * @author DuRuiChi
 * @create 2026-05-06
 */
@Service("fileServiceAsync")
@Slf4j
public class FileServiceAsyncImpl {

    @Resource
    private OSSConfig ossConfig;

    @Resource
    private FileUploadProducer fileUploadProducer;

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + File.separator + "richcodeweaver";

    /**
     * 异步上传文件到OSS
     * 先保存到本地临时目录，然后发送消息到队列
     *
     * @param file 上传的文件
     * @return 临时文件标识(messageId)
     */
    public String uploadAsync(MultipartFile file) {
        // 参数校验
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR, "上传文件不能为空");

        // 获取原生文件名
        String originalFilename = file.getOriginalFilename();
        ThrowUtils.throwIf(originalFilename == null, ErrorCode.OPERATION_ERROR, "无效的原生文件");

        try {
            // 1. 生成临时文件路径
            String tempFileName = UUID.randomUUID().toString(true) + "_" + originalFilename;
            File tempDir = new File(TEMP_DIR);
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            File tempFile = new File(tempDir, tempFileName);

            // 2. 保存到本地临时目录
            file.transferTo(tempFile);
            log.info("文件保存到临时目录成功: {}", tempFile.getAbsolutePath());

            // 3. 生成OSS存储路径
            LocalDateTime time = LocalDateTime.now();
            DateTimeFormatter dft = DateTimeFormatter.ofPattern("yyyy/MM/dd");
            String folder = dft.format(time);
            String fileName = generateUUID();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String ossPath = "richcodeweaver/" + folder + "/" + fileName + extension;

            // 4. 发送上传任务到消息队列
            String messageId = fileUploadProducer.sendUploadTask(
                    tempFile.getAbsolutePath(),
                    ossPath,
                    ossConfig.getBucketName(),
                    null // 用户ID可选
            );

            if (StrUtil.isBlank(messageId)) {
                // 发送失败，删除临时文件
                FileUtil.del(tempFile);
                throw new RuntimeException("上传任务发送失败");
            }

            // 5. 返回消息ID(前端可用于查询上传状态)
            log.info("文件上传任务已提交: messageId={}, originalFilename={}", messageId, originalFilename);
            return messageId;

        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取随机字符串
     */
    private String generateUUID() {
        return UUID.randomUUID()
                .toString()
                .replaceAll("-", "")
                .substring(0, 32);
    }
}
