package com.rich.richcodeweaver.common.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 删除响应类
 *
 * @author DuRuiChi
 * @create 2025/8/4
 **/
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}