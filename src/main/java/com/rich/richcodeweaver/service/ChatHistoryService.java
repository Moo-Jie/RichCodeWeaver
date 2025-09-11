package com.rich.richcodeweaver.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.rich.richcodeweaver.model.dto.chathistory.ChatHistoryQueryRequest;
import com.rich.richcodeweaver.model.entity.ChatHistory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层
 *
 * @author DuRuiChi
 * @return
 * @create 2025/8/16
 **/
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 添加对话历史
     *
     * @param appId       应用 id
     * @param message     消息
     * @param messageType 消息类型
     * @param userId      用户 id
     * @return boolean
     * @author DuRuiChi
     * @create 2025/8/16
     **/
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 根据应用 id 删除对话历史
     *
     * @param appId 应用 id
     * @return boolean
     * @author DuRuiChi
     * @create 2025/8/16
     **/
    boolean deleteByAppId(Long appId);

    /**
     * 分页查询某 APP 的对话记录
     *
     * @param appId          应用 id
     * @param pageSize       页面大小
     * @param lastCreateTime 最后创建时间
     * @param request        请求对象
     * @return com.mybatisflex.core.paginate.Page<com.rich.richcodeweaver.model.entity.ChatHistory> 分页结果
     * @author DuRuiChi
     * @create 2025/8/16
     **/
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               HttpServletRequest request);

    /**
     * 构造查询条件
     *
     * @param chatHistoryQueryRequest 查询请求对象
     * @return com.mybatisflex.core.query.QueryWrapper  查询包装类
     * @author DuRuiChi
     * @create 2025/8/16
     **/
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    /**
     * 从数据库中加载对话历史到记忆中
     *
     * @param appId      应用 id
     * @param chatMemory 对话内存
     * @param maxCount   加载数量
     * @return Boolean 是否加载成功
     * @author DuRuiChi
     * @create 2025/8/16
     **/
    Boolean loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}
