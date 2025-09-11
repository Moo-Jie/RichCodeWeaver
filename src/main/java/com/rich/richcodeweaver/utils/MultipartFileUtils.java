package com.rich.richcodeweaver.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

/**
 * MultipartFile和File互转工具类
 */
public class MultipartFileUtils {

    /**
     * 输入流转MultipartFile
     *
     * @param fileName
     * @param inputStream
     * @return
     */
    public static MultipartFile getMultipartFile(String fileName, InputStream inputStream) {
        MultipartFile multipartFile = null;
        try {
            multipartFile = new MockMultipartFile(fileName, fileName,
                    ContentType.APPLICATION_OCTET_STREAM.toString(), inputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return multipartFile;
    }

    /**
     * 读取网络文件
     *
     * @param url      文件地址
     * @param fileName 文件名称（需带文件名后缀）
     * @return
     */
    public static MultipartFile getMultipartFile(String url, String fileName) {
        HttpResponse response = HttpRequest.get(url).execute();
        InputStream inputStream = response.bodyStream();
        return MultipartFileUtils.getMultipartFile(fileName, inputStream);
    }

    /**
     * File 转 MultipartFile
     *
     * @param file
     * @return
     */
    public static MultipartFile getMultipartFile(File file) {
        FileInputStream fileInputStream = null;
        MultipartFile multipartFile = null;
        try {
            fileInputStream = new FileInputStream(file);
            multipartFile = new MockMultipartFile(file.getName(), file.getName(),
                    ContentType.APPLICATION_OCTET_STREAM.toString(), fileInputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return multipartFile;
    }

    /**
     * MultipartFileUtils 转File
     *
     * @param multipartFile
     * @return
     */
    public static File getFile(MultipartFile multipartFile) {
        String fileName = multipartFile.getOriginalFilename();
        File file = new File(fileName);
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            byte[] ss = multipartFile.getBytes();
            for (byte s : ss) {
                out.write(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
//        File f = new File(file.toURI());
//        if (f.delete()) {
//            System.out.println("删除成功");
//        } else {
//            System.out.println("删除失败");
//        }
        return file;
    }
}