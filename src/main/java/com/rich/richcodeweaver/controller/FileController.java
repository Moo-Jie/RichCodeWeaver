package com.rich.richcodeweaver.controller;

import com.rich.richcodeweaver.annotation.RateLimit;
import com.rich.richcodeweaver.exception.ErrorCode;
import com.rich.richcodeweaver.exception.ThrowUtils;
import com.rich.richcodeweaver.model.common.BaseResponse;
import com.rich.richcodeweaver.model.enums.RateLimitTypeEnum;
import com.rich.richcodeweaver.service.FileService;
import com.rich.richcodeweaver.utils.ResultUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private FileService fileService;

    /**
     * 文件上传接口
     *
     * @param file 上传的文件
     * @return com.rich.richcodeweaver.model.common.BaseResponse<java.lang.String>  文件上传成功后的 URL
     * @author DuRuiChi
     * @create 2025/9/3
     **/
   @RateLimit(type = RateLimitTypeEnum.API, rate = 30, window = 10)
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
