package com.rich.codeweaver.service.generator;

import com.rich.codeweaver.model.entity.App;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 下载代码文件 服务层
 * 提供代码文件下载功能
 *
 * @author DuRuiChi
 * @since 2026-03-10
 */
public interface DownloadCodeFileService {
    /**
     * 下载代码文件（ZIP格式）
     *
     * @param app      产物实体
     * @param response 响应对象
     * @author DuRuiChi
     */
    void downloadCodeZipFile(App app, HttpServletResponse response);
}
