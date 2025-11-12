package com.rich.richcodeweaver.config;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OSS配置类
 * （仅在配置文件中配置了 aliyun.oss 相关属性时生效）
 *
 * @author DuRuiChi
 * @create 2025/9/3
 **/
@ConfigurationProperties(prefix = "aliyun.oss")
@Configuration
@ConditionalOnProperty(
        prefix = "aliyun.oss",
        name = {"endPoint", "accessKeyId", "accessKeySecret", "bucketName"}
)
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
