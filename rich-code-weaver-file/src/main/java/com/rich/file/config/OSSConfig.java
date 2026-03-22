package com.rich.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OSS配置类
 * 从配置文件中读取阿里云OSS相关配置属性
 *
 * @author DuRuiChi
 * @since 2026-03-11
 */
@ConfigurationProperties(prefix = "aliyun.oss")
@Configuration
@Data
public class OSSConfig {

    /**
     * OSS 服务器所在区域节点
     */
    private String endPoint;
    /**
     * 桶 ID
     */
    private String accessKeyId;
    /**
     * 密钥
     */
    private String accessKeySecret;
    /**
     * 存储空间名称
     */
    private String bucketName;
}
