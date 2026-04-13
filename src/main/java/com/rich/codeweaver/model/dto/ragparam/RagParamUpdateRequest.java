package com.rich.codeweaver.model.dto.ragparam;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * RAG 参数配置 更新请求
 * 仅允许修改参数值，参数键/类型/分组由系统初始化固定
 *
 * @author DuRuiChi
 * @create 2026/3/27
 */
@Data
public class RagParamUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 参数 id
     */
    private Long id;

    /**
     * 新的参数值（字符串形式，后端按 paramType 校验格式）
     */
    private String paramValue;
}
