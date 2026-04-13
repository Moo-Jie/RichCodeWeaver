package com.rich.codeweaver.controller.file;

import com.rich.codeweaver.service.generator.FileService;
import com.rich.codeweaver.common.model.BaseResponse;
import com.rich.codeweaver.common.utils.ResultUtils;
import com.rich.codeweaver.common.exception.ErrorCode;
import com.rich.codeweaver.common.exception.ThrowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * 文件上传接口
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
