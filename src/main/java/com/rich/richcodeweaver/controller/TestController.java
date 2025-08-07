package com.rich.richcodeweaver.controller;

import com.rich.richcodeweaver.model.common.BaseResponse;
import com.rich.richcodeweaver.utiles.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class TestController {

    @GetMapping("/")
    public BaseResponse<String> healthCheck() {
        return ResultUtils.success("The server request processing status is healthy");
    }
}
