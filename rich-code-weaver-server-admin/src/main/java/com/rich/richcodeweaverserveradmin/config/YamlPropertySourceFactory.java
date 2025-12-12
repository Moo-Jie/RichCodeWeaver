package com.rich.richcodeweaverserveradmin.config;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * YAML 属性源工厂
 * 用于支持 @PropertySource 注解读取 YAML 文件
 * 
 * @author Rich
 * @date 2025-12-12
 */
public class YamlPropertySourceFactory implements PropertySourceFactory {
    
    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(resource.getResource());
        
        Properties properties = factory.getObject();
        
        return new PropertiesPropertySource(
            resource.getResource().getFilename(),
            properties
        );
    }
}

