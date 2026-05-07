package com.rich.file.controller;

import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.common.model.BaseResponse;
import com.rich.common.utils.ResultUtils;
import com.rich.file.service.FileService;
import com.rich.file.service.impl.FileServiceAsyncImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * 文件控制器
 * 提供文件上传等接口
 *
 * @author DuRuiChi
 * @since 2026-03-11
 */
@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private FileService fileService;

    @Resource
    private FileServiceAsyncImpl fileServiceAsync;

    /**
     * 文件上传接口(同步版本，保持向后兼容)
     *
     * @param file 上传的文件
     * @return 文件上传成功后的URL
     * @author DuRuiChi
     */
//    @RateLimit(type = RateLimitTypeEnum.API, rate = 30, window = 10)
    @PostMapping("/upload")
    public BaseResponse<String> upload(@RequestPart("file") MultipartFile file) {
        {
            // 参数校验
            ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR, "上传文件不能为空");
            // 上传文件
            String imgFileStr = fileService.upload(file);
            return ResultUtils.success(imgFileStr);
        }
    }

    /**
     * 文件上传接口(异步版本，推荐使用)
     * 文件先保存到临时目录，然后发送到消息队列异步上传
     *
     * @param file 上传的文件
     * @return 消息ID(可用于查询上传状态)
     * @author DuRuiChi
     * @create 2026/5/6
     */
    @PostMapping("/upload/async")
    public BaseResponse<String> uploadAsync(@RequestPart("file") MultipartFile file) {
        // 参数校验
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        // 异步上传文件
        String messageId = fileServiceAsync.uploadAsync(file);
        return ResultUtils.success(messageId);
    }

    @GetMapping("/proxy/image")
    public ResponseEntity<ByteArrayResource> proxyImage(@RequestParam("url") String url) throws Exception {
        ThrowUtils.throwIf(url == null || url.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "图片地址不能为空");

        URL imageUrl = new URL(url);
        URLConnection connection = imageUrl.openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        String contentType = connection.getContentType();
        byte[] bytes;
        try (InputStream inputStream = connection.getInputStream()) {
            bytes = inputStream.readAllBytes();
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (contentType != null && !contentType.trim().isEmpty()) {
            mediaType = MediaType.parseMediaType(contentType);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=600")
                .contentType(mediaType)
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }
}
