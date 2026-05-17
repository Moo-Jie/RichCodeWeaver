package com.rich.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 对话增量摘要 实体类
 * 存储每个会话的近期对话摘要，用于 LLM 上下文补充
 *
 * @author DuRuiChi
 * @create 2026/5/17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("ai_chat_summary")
public class AiChatSummary implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id（雪花）
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 记忆标识（Agent/Workflow 对应 appId，CustomerService 对应 conversationId）
     */
    @Column("memoryId")
    private Long memoryId;

    /**
     * 摘要类型：AGENT / WORKFLOW / CUSTOMER_SERVICE
     */
    @Column("summaryType")
    private String summaryType;

    /**
     * 摘要内容（结构化文本）
     */
    @Column("summaryContent")
    private String summaryContent;

    /**
     * 已摘要的累计对话轮数（每生成一次摘要时更新为当前总轮数）
     */
    @Column("summarizedRoundCount")
    private Integer summarizedRoundCount;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column("updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
