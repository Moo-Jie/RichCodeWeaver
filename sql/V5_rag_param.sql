-- ============================================================
-- V5: RAG 参数配置表
-- 将 RagDocumentIndexCreationService 和 RagContentRetrieverAugmentorFactory
-- 中写死的参数迁移至数据库，支持运行时动态调整
-- ============================================================

CREATE TABLE IF NOT EXISTS rag_param
(
    id          BIGINT       NOT NULL PRIMARY KEY COMMENT 'id（雪花）',
    param_key   VARCHAR(64)  NOT NULL UNIQUE COMMENT '参数键（唯一标识，代码中使用）',
    param_name  VARCHAR(128) NOT NULL COMMENT '参数名称（中文，供管理界面展示）',
    param_value TEXT         NOT NULL COMMENT '参数值（统一以字符串存储，按 param_type 转换）',
    param_type  VARCHAR(16)  NOT NULL COMMENT '参数类型：int / double / textarea',
    param_group VARCHAR(64)  NOT NULL COMMENT '参数分组：etl / retrieval / injection',
    description VARCHAR(512)          COMMENT '参数描述（用途说明）',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序（越小越靠前）',
    create_time DATETIME              COMMENT '创建时间',
    update_time DATETIME              COMMENT '更新时间'
) COMMENT = 'RAG 参数配置表';

-- ── ETL 阶段：文档切片参数 ─────────────────────────────────
INSERT INTO rag_param (id, param_key, param_name, param_value, param_type, param_group, description, sort_order,
                       create_time, update_time)
VALUES (1860000000000001, 'max_segment_size', '切片最大字符数', '500', 'int', 'etl',
        '每个文本片段的最大字符数。500 个中文字符约等于 200-300 token，能覆盖一个完整小节。过大检索精度下降，过小丢失上下文。',
        1, NOW(), NOW()),

       (1860000000000002, 'max_overlap_size', '切片重叠字符数', '50', 'int', 'etl',
        '相邻切片之间的重叠字符数，用于保留跨片段的上下文连续性。约为切片大小的 10%，兼顾上下文保留和存储效率。',
        2, NOW(), NOW());

-- ── 检索阶段：向量搜索参数 ───────────────────────────────────
INSERT INTO rag_param (id, param_key, param_name, param_value, param_type, param_group, description, sort_order,
                       create_time, update_time)
VALUES (1860000000000003, 'max_results', '最大检索结果数（Top-K）', '5', 'int', 'retrieval',
        '每次 RAG 检索返回的最大文档片段数量。过多增加 token 消耗和噪声，过少可能遗漏关键信息。推荐范围：3~10。',
        3, NOW(), NOW()),

       (1860000000000004, 'min_score', '最低相似度阈值', '0.6', 'double', 'retrieval',
        '检索结果的最低余弦相似度分数（范围 0~1）。低于此值的片段将被过滤，避免注入无关内容。推荐范围：0.5~0.8。',
        4, NOW(), NOW());

-- ── 注入阶段：提示词模板 ──────────────────────────────────────
INSERT INTO rag_param (id, param_key, param_name, param_value, param_type, param_group, description, sort_order,
                       create_time, update_time)
VALUES (1860000000000005, 'injection_prompt_template',
        'RAG 注入提示词模板', '{{userMessage}}

---
【权威参考知识】以下内容来自系统知识库，是你生成代码时必须严格遵循的开发规范与约束，不可违反或忽略：

{{contents}}
---', 'textarea', 'injection',
        'RAG 内容注入的提示词模板。{{userMessage}} 占位符替换为用户原始消息，{{contents}} 占位符替换为检索到的知识库片段。',
        5, NOW(), NOW());
