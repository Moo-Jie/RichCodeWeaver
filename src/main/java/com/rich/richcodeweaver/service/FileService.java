package com.rich.richcodeweaver.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 阿里云 OSS 文件上传服务
 *
 * @author DuRuiChi
 * @create 2025/9/3
 **/

public interface FileService {

    /**
     * 阿里云OSS文件上传
     *
     * @param file
     * @return
     */
    String upload(MultipartFile file);
}
