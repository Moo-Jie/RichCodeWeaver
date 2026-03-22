package com.rich.model.dto.app;

import com.rich.model.enums.CodeGeneratorTypeEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * 产物创建请求
 */
@Data
public class AppAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 产物初始化的用户 prompt
     */
    private String initPrompt;
    /**
     * 产物生成类型
     */
    private CodeGeneratorTypeEnum generatorType;
} 