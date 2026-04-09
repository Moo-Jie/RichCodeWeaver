package com.rich.app.rag;

import com.rich.ai.rag.RagParamProvider;
import com.rich.app.service.RagParamService;
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

    @Override
    public int getMaxSegmentSize() {
        return ragParamService.getIntParam("max_segment_size", 500);
    }

    @Override
    public int getMaxOverlapSize() {
        return ragParamService.getIntParam("max_overlap_size", 50);
    }

    @Override
    public int getMaxResults() {
        return ragParamService.getIntParam("max_results", 5);
    }

    @Override
    public double getMinScore() {
        return ragParamService.getDoubleParam("min_score", 0.6);
    }

    @Override
    public String getInjectionPromptTemplate() {
        return ragParamService.getParamValue("injection_prompt_template", DEFAULT_INJECTION_TEMPLATE);
    }
}
