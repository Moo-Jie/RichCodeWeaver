package com.rich.richcodeweaver.model.aiChatResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用于 AI 代码审查模型输出 【代码审查结果】 的响应结果
 *
 * @author DuRuiChi
 * @return
 * @create 2025/9/18
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeReviewResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否通过代码审查
     */
    private Boolean isPass;

    /**
     * 错误列表
     */
    private List<String> errorList;

    /**
     * 改进建议
     */
    private List<String> suggestionList;

    /**
     * 审查批次
     */
    private Long reviewCount = 0L;
}
