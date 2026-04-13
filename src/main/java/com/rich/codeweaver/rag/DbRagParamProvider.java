package com.rich.codeweaver.rag;

import com.rich.codeweaver.common.constant.RagConstant;
import com.rich.codeweaver.service.generator.RagParamService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 基于数据库的 RAG 参数提供者实现
 *
 * @author DuRuiChi
 * @create 2026/3/27
 * @see RagParamProvider 接口定义
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class DbRagParamProvider implements RagParamProvider {

    private static final String DEFAULT_INJECTION_TEMPLATE =
            "{{userMessage}}\n\n" +
                    "---\n" +
                    "【权威参考知识】以下内容来自系统知识库，是你生成代码时必须严格遵循的开发规范与约束，不可违反或忽略：\n\n" +
                    "{{contents}}\n" +
                    "---";

    @Resource
    private RagParamService ragParamService;

    /**
     * 获取分段时的最大块大小
     *
     * @return 最大块大小
     */
    @Override
    public int getMaxSegmentSize() {
        return ragParamService.getIntParam(RagConstant.PARAM_MAX_SEGMENT_SIZE, RagConstant.DEFAULT_MAX_SEGMENT_SIZE);
    }

    /**
     * 获取分段时的最大重叠大小
     *
     * @return 最大重叠大小
     */
    @Override
    public int getMaxOverlapSize() {
        return ragParamService.getIntParam(RagConstant.PARAM_MAX_OVERLAP_SIZE, RagConstant.DEFAULT_MAX_OVERLAP_SIZE);
    }

    /**
     * 获取单次检索的最大结果数
     *
     * @return 最大结果数
     */
    @Override
    public int getMaxResults() {
        return ragParamService.getIntParam(RagConstant.PARAM_MAX_RESULTS, RagConstant.DEFAULT_MAX_RESULTS);
    }

    /**
     * 获取检索结果最低分数阈值
     *
     * @return 最低分数阈值
     */
    @Override
    public double getMinScore() {
        return ragParamService.getDoubleParam(RagConstant.PARAM_MIN_SCORE, RagConstant.DEFAULT_MIN_SCORE);
    }

    /**
     * 获取知识注入模板
     *
     * @return 注入模板
     */
    @Override
    public String getInjectionPromptTemplate() {
        return ragParamService.getParamValue(RagConstant.PARAM_INJECTION_PROMPT_TEMPLATE, DEFAULT_INJECTION_TEMPLATE);
    }
}
