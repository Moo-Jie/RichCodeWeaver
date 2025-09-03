package com.rich.richcodeweaver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OSS配置类
 *
 * @author DuRuiChi
 * @create 2025/9/3
 **/
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
