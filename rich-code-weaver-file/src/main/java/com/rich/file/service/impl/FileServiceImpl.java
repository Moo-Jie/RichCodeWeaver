package com.rich.file.service.impl;

import cn.hutool.core.lang.UUID;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectResult;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.file.config.OSSConfig;
import com.rich.file.service.FileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 阿里云 OSS 文件上传实现类
 *
 * @author lixiang
 */
@Service
@Slf4j
public class FileServiceImpl implements FileService {

    /**
     * 注入 OSS 配置
     **/
    @Resource
    private OSSConfig ossConfig;

    /**
     * 阿里云 OSS 文件上传，含 配置注入、OSS 客户端服务实例的创建、文件上传逻辑的具体实现
     *
     * @param file 上传的文件
     * @return java.lang.String  文件上传后的路径
     * @author DuRuiChi
     * @create 2025/9/3
     **/
    @Override
    public String upload(MultipartFile file) {
        // 参数校验
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        // 获取相关配置
        String bucketName = ossConfig.getBucketName();
        String endPoint = ossConfig.getEndPoint();
        String accessKeyId = ossConfig.getAccessKeyId();
        String accessKeySecret = ossConfig.getAccessKeySecret();

        // 校验配置
        ThrowUtils.throwIf(bucketName == null || endPoint == null || accessKeyId == null || accessKeySecret == null,
                ErrorCode.PARAMS_ERROR, "OSS 配置为空，请检查是否填写正确");

        // 创建 OSS 对象
        OSS ossClient = new OSSClientBuilder().build(endPoint, accessKeyId, accessKeySecret);

        // 获取原生文件名
        String originalFilename = file.getOriginalFilename();
        ThrowUtils.throwIf(originalFilename == null, ErrorCode.OPERATION_ERROR, "无效的原生文件");
        // JDK8+ 的日期格式
        LocalDateTime time = LocalDateTime.now();
        DateTimeFormatter dft = DateTimeFormatter.ofPattern("yyyy/MM/dd");

        // 拼装 OSS 上存储的路径
        String folder = dft.format(time);
        String fileName = generateUUID();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

        // 在 OSS 上 bucket 下的文件名
        String uploadFileName = "richcodeweaver/" + folder + "/" + fileName + extension;

        try {
            PutObjectResult result = ossClient.putObject(bucketName, uploadFileName, file.getInputStream());
            // 拼装返回路径
            if (result != null) {
                return "https://" + bucketName + "." + endPoint + "/" + uploadFileName;
            }
        } catch (IOException e) {
            log.error("文件上传失败:{}", e.getMessage());
            throw new RuntimeException("文件上传失败,因为：" + e.getMessage());
        } finally {
            // OSS 关闭服务，不然会造成 OOM
            ossClient.shutdown();
        }
        return null;
    }

    /**
     * 获取随机字符串
     *
     * @return java.lang.String  随机字符串
     * @author DuRuiChi
     * @create 2025/9/3
     **/
    private String generateUUID() {
        return UUID.randomUUID()
                .toString()
                .replaceAll("-", "")
                .substring(0, 32);
    }
}
