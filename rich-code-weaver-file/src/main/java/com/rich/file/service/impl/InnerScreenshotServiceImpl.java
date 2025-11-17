package com.rich.file.service.impl;

import com.rich.client.innerService.InnerScreenshotService;
import com.rich.file.service.ScreenshotService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 提供外部调用截图服务的实现类
 * （由 Dubbo 提供服务发现和调用）
 *
 * @author DuRuiChi
 * @create 2025/11/17
 **/
@DubboService
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
