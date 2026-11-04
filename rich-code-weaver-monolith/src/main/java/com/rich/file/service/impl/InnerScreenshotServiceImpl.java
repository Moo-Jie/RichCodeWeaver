package com.rich.file.service.impl;

import com.rich.file.api.InnerScreenshotService;
import com.rich.file.service.ScreenshotService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 提供本地截图内部服务的实现类
 * （由本地 Spring Bean 提供）
 *
 * @author DuRuiChi
 * @create 2025/10/17
 **/
@Service
public class InnerScreenshotServiceImpl implements InnerScreenshotService {
    /**
     * 调用模块内部的实现类
     */
    @Resource
    private ScreenshotService screenshotService;

    @Override
    public String generateAndUploadScreenshot(String webUrl) {
        return screenshotService.generateAndUploadScreenshot(webUrl);
    }
}
