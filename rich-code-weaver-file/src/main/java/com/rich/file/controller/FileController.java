package com.rich.file.controller;

import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.common.model.BaseResponse;
import com.rich.common.utils.ResultUtils;
import com.rich.file.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
}
