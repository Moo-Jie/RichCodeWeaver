package com.rich.richcodeweaver.model.dto.app;

import com.rich.richcodeweaver.model.enums.CodeGeneratorTypeEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * 应用创建请求
 */
@Data
public class AppAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 应用初始化的用户 prompt
     */
    private String initPrompt;
    /**
     * 应用生成类型
     */
    private CodeGeneratorTypeEnum generatorType;
} 