package com.rich.client.innerService;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务内部接口
 * 通过Dubbo提供文件上传服务的远程调用接口
 *
 * @author DuRuiChi
 * @since 2026-03-11
 */
public interface InnerFileService {
    /**
     * 文件上传
     *
     * @param file 上传的文件
     * @return java.lang.String  文件上传后的路径
     */
    String upload(MultipartFile file);

    /**
     * 文件上传（字节数组方式，用于Dubbo RPC调用）
     *
     * @param fileBytes 文件字节数组
     * @param fileName  文件名（包含扩展名）
     * @return java.lang.String  文件上传后的路径
     */
    String uploadBytes(byte[] fileBytes, String fileName);
}
