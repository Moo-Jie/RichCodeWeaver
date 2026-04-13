package com.rich.codeweaver.service.generator;

import org.springframework.web.multipart.MultipartFile;

/**
 * 阿里云 OSS 文件上传服务
 * 提供文件上传到阿里云OSS的业务逻辑
 *
 * @author DuRuiChi
 * @since 2026-03-11
 */
public interface FileService {

    /**
     * 阿里云OSS文件上传
     *
     * @param file 待上传的文件
     * @return 文件访问URL
     */
    String upload(MultipartFile file);


    String uploadBytes(byte[] fileBytes, String fileName);
}
