package com.rich.richcodeweaver.model.dto.chathistory;

import com.rich.richcodeweaver.model.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话历史查询响应类
 *
 * @author DuRuiChi
 * @create 2025/8/16
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class ChatHistoryQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;
    /**
     * 本地时间戳
     * （用于游标分页查询，获取早于此时间戳的记录）
     */
    private LocalDateTime lastCreateTime;
    /**
     * 消息内容
     */
    private String message;
    /**
     * 消息类型
     */
    private String messageType;
    /**
     * 应用id
     */
    private Long appId;
    /**
     * 创建用户id
     */
    private Long userId;
}