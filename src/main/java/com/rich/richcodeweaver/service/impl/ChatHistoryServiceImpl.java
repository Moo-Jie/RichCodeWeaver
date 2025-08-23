package com.rich.richcodeweaver.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.richcodeweaver.constant.UserConstant;
import com.rich.richcodeweaver.exception.ErrorCode;
import com.rich.richcodeweaver.exception.ThrowUtils;
import com.rich.richcodeweaver.mapper.ChatHistoryMapper;
import com.rich.richcodeweaver.model.dto.chathistory.ChatHistoryQueryRequest;
import com.rich.richcodeweaver.model.entity.App;
import com.rich.richcodeweaver.model.entity.ChatHistory;
import com.rich.richcodeweaver.model.entity.User;
import com.rich.richcodeweaver.model.enums.ChatHistoryTypeEnum;
import com.rich.richcodeweaver.service.AppService;
import com.rich.richcodeweaver.service.ChatHistoryService;
import com.rich.richcodeweaver.service.UserService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史 服务层实现。
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 */
@Slf4j
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Resource
    @Lazy
    private AppService appService;

    @Resource
    private UserService userService;

    /**
     * 添加对话消息
     *
     * @param appId       应用ID
     * @param message     消息内容
     * @param messageType 消息类型
     * @param userId      用户ID
     * @return boolean
     * @author DuRuiChi
     * @create 2025/8/16
     **/
    @Override
    public boolean addChatMessage(Long appId, String message, String messageType, Long userId) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容为空");
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "消息类型为空");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID为空");
        ChatHistoryTypeEnum messageTypeEnum = ChatHistoryTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "非法消息类型");
        // 执行插入，当重复时更新
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .message(message)
                .messageType(messageType)
                .userId(userId)
                .build();
        return this.save(chatHistory);
    }

    /**
     * 根据应用ID删除对话历史
     *
     * @param appId 应用ID
     * @return boolean
     * @author DuRuiChi
     * @create 2025/8/16
     **/
    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);
        return this.remove(queryWrapper);
    }

    /**
     * 分页查询某 APP 的对话记录
     *
     * @param appId          应用ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后创建时间
     * @param request        HTTP请求
     * @return Page<ChatHistory>
     * @author DuRuiChi
     * @create 2025/8/16
     **/
    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                                      LocalDateTime lastCreateTime,
                                                      HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 验证权限及应用从属
        ThrowUtils.throwIf(!UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())
                        && !app.getUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "无权查看该应用的对话历史");
        // 构建查询条件
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        // 查询数据
        return this.page(Page.of(1, pageSize), queryWrapper);
    }

    /**
     * 构造查询条件
     *
     * @param chatHistoryQueryRequest 对话历史查询请求
     * @return com.mybatisflex.core.query.QueryWrapper
     * @author DuRuiChi
     * @create 2025/8/16
     **/
    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        // 设置查询条件
        queryWrapper.eq("id", id)
                .like("message", message)
                .eq("messageType", messageType)
                .eq("appId", appId)
                .eq("userId", userId);
        // 设置游标查询，防止重复查询
        if (lastCreateTime != null) {
            // 查询指定时间戳游标之前的记录
            queryWrapper.lt("createTime", lastCreateTime);
        }
        // 设置排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认按创建时间降序排列
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;
    }

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
    @Override
    public Boolean loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            // 跳过第1条（最新记录），加载后续 maxCount 条，从而排除当前未处理的最新消息，避免重复加载
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);
            List<ChatHistory> historyList = this.list(queryWrapper);
            if (CollUtil.isEmpty(historyList)) {
                return false;
            }
            // 反转列表，反转为正序（旧→新）
            historyList = historyList.reversed();
            log.info("为 appId: {} 加载 {} 条历史记录", appId, historyList.size());
            // 先清理历史缓存，防止重复加载
            chatMemory.clear();
            // 加载历史记录到记忆中
            // UserMessage.from() 方法用于创建用户消息 ; chatMemory.add() 方法用于顺序添加历史消息到 chatMemory 中
            for (ChatHistory history : historyList) {
                // 区分对话类型
                if (ChatHistoryTypeEnum.USER.getValue().equals(history.getMessageType())) {
                    // 加载用户消息
                    chatMemory.add(UserMessage.from(history.getMessage()));
                } else if (ChatHistoryTypeEnum.AI.getValue().equals(history.getMessageType())) {
                    // 加载 AI 消息
                    chatMemory.add(AiMessage.from(history.getMessage()));
                }
            }
            return true;
        } catch (Exception e) {
            log.error("【AI 服务实例缓存】加载对话历史失败，appId: {}，因为:{}", appId, e.getMessage());
            return false;
        }
    }
}
