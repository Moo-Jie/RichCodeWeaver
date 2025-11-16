package com.rich.app.service;

import com.rich.model.entity.App;
import jakarta.servlet.http.HttpServletResponse;

public interface DownloadCodeFileService {
    /**
     * 下载代码文件
     *
     * @param app      应用
     * @param response 响应信息
     */
    void downloadCodeZipFile(App app, HttpServletResponse response);
}
