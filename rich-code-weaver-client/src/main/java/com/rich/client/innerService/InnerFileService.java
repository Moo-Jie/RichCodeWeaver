package com.rich.client.innerService;

import org.springframework.web.multipart.MultipartFile;

/**
 * Web 网页截图服务内部接口
 * （用于系统内部调用的客户端）
 *
 * @author DuRuiChi
 * @create 2025/11/17
 **/
public interface InnerFileService {
    /**
     * 文件上传
     *
     * @param file 上传的文件
     * @return java.lang.String  文件上传后的路径
     */
    String upload(MultipartFile file);
}
