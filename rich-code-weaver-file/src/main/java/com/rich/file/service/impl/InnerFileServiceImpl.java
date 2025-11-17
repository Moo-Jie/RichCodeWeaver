package com.rich.file.service.impl;

import com.rich.client.innerService.InnerFileService;
import com.rich.file.service.FileService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务内部接口
 * （用于系统内部调用的客户端）
 *
 * @author DuRuiChi
 * @create 2025/11/17
 **/
@DubboService
public class InnerFileServiceImpl implements InnerFileService {
    /**
     * 调用模块内部的实现类
     */
    @Resource
    private FileService fileService;

    @Override
    public String upload(MultipartFile file) {
        return fileService.upload(file);
    }
}
