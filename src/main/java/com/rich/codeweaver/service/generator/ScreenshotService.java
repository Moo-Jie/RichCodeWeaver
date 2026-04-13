package com.rich.codeweaver.service.generator;

/**
 * Web 网页截图服务接口
 * 提供网页截图生成和上传的业务逻辑
 *
 * @author DuRuiChi
 * @since 2026-03-11
 */
public interface ScreenshotService {
    /**
     * 生成并上传网页截图
     *
     * @param webUrl 目标网页URL
     * @return 截图在对象存储的访问URL，失败返回null
     */
    String generateAndUploadScreenshot(String webUrl);
}
