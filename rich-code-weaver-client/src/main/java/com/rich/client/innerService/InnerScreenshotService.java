package com.rich.client.innerService;

/**
 * Web 网页截图服务内部接口
 * 通过Dubbo提供网页截图服务的远程调用接口
 *
 * @author DuRuiChi
 * @since 2026-03-11
 */
public interface InnerScreenshotService {
    /**
     * 生成并上传网页截图
     *
     * @param webUrl 目标网页URL
     * @return 截图在对象存储的访问URL，失败返回null
     */
    String generateAndUploadScreenshot(String webUrl);
}
