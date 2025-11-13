package com.rich.client.innerService;

/**
 * Web 网页截图服务内部接口
 * （用于系统内部调用的客户端）
 *
 * @author DuRuiChi
 * @create 2025/11/12
 **/
public interface InnerScreenshotService {
    /**
     * 生成并上传网页截图
     *
     * @param webUrl 目标网页URL
     * @return 截图在对象存储的访问URL，失败返回null
     */
    String generateAndUploadScreenshot(String webUrl);
}
