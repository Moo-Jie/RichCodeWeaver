package com.rich.richcodeweaver.service;

/**
 * Web 网页截图服务接口
 *
 * @author DuRuiChi
 * @create 2025/8/31
 **/
public interface ScreenshotService {
    /**
     * 生成并上传网页截图
     *
     * @param webUrl 目标网页URL
     * @return 截图在对象存储的访问URL，失败返回null
     */
    String generateAndUploadScreenshot(String webUrl);
}
