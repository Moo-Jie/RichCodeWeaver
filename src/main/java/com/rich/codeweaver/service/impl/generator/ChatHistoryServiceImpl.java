package com.rich.codeweaver.service.impl.generator;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import com.rich.codeweaver.common.exception.ErrorCode;
import com.rich.codeweaver.common.exception.ThrowUtils;
import com.rich.codeweaver.mapper.ChatHistoryMapper;
import com.rich.codeweaver.model.dto.chathistory.ChatHistoryQueryRequest;
import com.rich.codeweaver.model.entity.App;
import com.rich.codeweaver.model.entity.ChatHistory;
import com.rich.codeweaver.model.entity.User;
import com.rich.codeweaver.model.enums.ChatHistoryTypeEnum;
import com.rich.codeweaver.service.generator.AppService;
import com.rich.codeweaver.service.generator.ChatHistoryService;
import com.rich.codeweaver.service.user.UserService;
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
 * 对话历史 服务层实现
 * 实现对话历史的增删改查等业务逻辑
 *
 * @author DuRuiChi
 * @since 2026-03-10
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
     * @param appId       产物ID
     * @param message     消息内容
     * @param messageType 消息类型
     * @param userId      用户ID
     * @return boolean
     * @author DuRuiChi
     * @create 2025/12/16
     **/
    @Override
    public boolean addChatMessage(Long appId, String message, String messageType, Long userId) {
        // 参数校验：验证产物ID有效性
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "产物ID无效");
        // 参数校验：验证消息内容不为空
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        // 参数校验：验证消息类型不为空
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        // 参数校验：验证用户ID有效性
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID无效");

        // 验证消息类型的合法性（必须是预定义的枚举值：USER 或 AI）
        ChatHistoryTypeEnum messageTypeEnum = ChatHistoryTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR,
                "消息类型不合法，必须是 USER 或 AI: " + messageType);

        // 构建对话历史实体对象
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .message(message)
                .messageType(messageType)
                .userId(userId)
                .build();

        // 保存对话历史到数据库
        boolean saveSuccess = this.save(chatHistory);

        // 记录保存结果日志
        if (saveSuccess) {
            log.debug("保存对话消息成功: appId={}, userId={}, messageType={}, messageLength={}",
                    appId, userId, messageType, message.length());
        } else {
            log.error("保存对话消息失败: appId={}, userId={}, messageType={}", appId, userId, messageType);
        }

        return saveSuccess;
    }

    /**
     * 根据产物ID删除对话历史
     *
     * @param appId 产物ID
     * @return boolean
     * @author DuRuiChi
     * @create 2025/12/16
     **/
    @Override
    public boolean deleteByAppId(Long appId) {
        // 参数校验：验证产物ID有效性
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "产物ID无效");

        // 构建删除条件：根据产物ID删除所有相关对话历史（级联删除）
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);

        // 执行批量删除操作
        boolean deleteSuccess = this.remove(queryWrapper);

        // 记录删除结果日志
        if (deleteSuccess) {
            log.info("删除产物对话历史成功: appId={}", appId);
        } else {
            log.warn("删除产物对话历史失败或无记录: appId={}", appId);
        }

        return deleteSuccess;
    }

    /**
     * 分页查询某 APP 的对话记录
     *
     * @param appId          产物ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后创建时间
     * @param request        HTTP请求
     * @return Page<ChatHistory>
     * @author DuRuiChi
     * @create 2025/12/16
     **/
    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                                      LocalDateTime lastCreateTime,
                                                      HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 参数校验：验证产物ID有效性
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "产物ID无效");
        // 参数校验：验证分页大小合理性（限制最大50条防止数据量过大，避免内存溢出）
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR,
                "页面大小必须在1-50之间，当前值: " + pageSize);
        // 参数校验：验证用户已登录
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        ThrowUtils.throwIf(loginUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR, "用户ID无效");

        // 查询产物信息并验证其存在性
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "产物不存在");

        // 权限校验（预留）
        // TODO: 开启权限控制时，只允许查看自己的产物对话历史
//        boolean isOwner = app.getUserId().equals(loginUser.getId());
//        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
//        ThrowUtils.throwIf(!isOwner && !isAdmin, ErrorCode.NO_AUTH_ERROR, "无权查看该产物的对话历史");

        // 构建查询请求对象
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);  // 游标分页：基于时间戳

        // 构建查询条件（支持游标分页，避免深分页性能问题）
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);

        // 执行分页查询（固定页码为1，通过游标实现分页）
        Page<ChatHistory> resultPage = this.page(Page.of(1, pageSize), queryWrapper);

        log.debug("查询产物对话历史: appId={}, pageSize={}, lastCreateTime={}, resultCount={}",
                appId, pageSize, lastCreateTime, resultPage.getRecords().size());

        return resultPage;
    }

    /**
     * 构造查询条件
     *
     * @param chatHistoryQueryRequest 对话历史查询请求
     * @return com.mybatisflex.core.query.QueryWrapper
     * @author DuRuiChi
     * @create 2025/12/16
     **/
    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        // 创建查询条件构造器
        QueryWrapper queryWrapper = QueryWrapper.create();

        // 空值保护：如果请求参数为空，返回空查询条件
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }

        // 解构查询请求参数
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();

        // 设置查询条件（仅当参数不为空时添加条件）
        queryWrapper.eq("id", id)  // 精确匹配：对话历史ID
                .like("message", message)  // 模糊查询：消息内容
                .eq("messageType", messageType)  // 精确匹配：消息类型（USER/AI）
                .eq("appId", appId)  // 精确匹配：产物ID
                .eq("userId", userId);  // 精确匹配：用户ID

        // 设置游标查询，防止重复查询（基于时间戳的游标分页）
        if (lastCreateTime != null) {
            // 查询指定时间戳游标之前的记录（降序排列时，查询更早的记录）
            queryWrapper.lt("createTime", lastCreateTime);
        }

        // 设置排序规则
        if (StrUtil.isNotBlank(sortField)) {
            // 自定义排序字段和排序方向
            boolean isAscend = "ascend".equals(sortOrder);
            queryWrapper.orderBy(sortField, isAscend);
        } else {
            // 默认按创建时间降序排列（最新的消息在前）
            queryWrapper.orderBy("createTime", false);
        }

        return queryWrapper;
    }

    /**
     * 从数据库中加载对话历史到记忆中
     *
     * @param appId      产物 id
     * @param chatMemory 对话内存
     * @param maxCount   加载数量
     * @return Boolean 是否加载成功
     * @author DuRuiChi
     * @create 2025/12/16
     **/
    @Override
    public Boolean loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            // 参数校验：确保产物ID、聊天记忆对象和加载数量有效
            if (appId == null || appId <= 0) {
                log.warn("加载对话历史失败：产物ID无效 - appId={}", appId);
                return false;
            }
            if (chatMemory == null) {
                log.warn("加载对话历史失败：聊天记忆对象为空 - appId={}", appId);
                return false;
            }
            if (maxCount <= 0) {
                log.warn("加载对话历史失败：加载数量无效 - appId={}, maxCount={}", appId, maxCount);
                return false;
            }

            // 构建查询条件：跳过第1条（最新记录），加载后续 maxCount 条
            // 原因：排除当前未处理的最新消息，避免重复加载到上下文中
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)  // 筛选指定产物的对话历史
                    .orderBy(ChatHistory::getCreateTime, false)  // 按创建时间降序排列（最新在前）
                    .limit(1, maxCount);  // 跳过第1条，取后续maxCount条

            // 查询对话历史记录
            List<ChatHistory> historyList = this.list(queryWrapper);
            if (CollUtil.isEmpty(historyList)) {
                log.info("加载对话历史：无历史记录 - appId={}", appId);
                return false;
            }

            // 反转列表，转换为正序（旧→新），符合对话上下文的时间顺序
            historyList = historyList.reversed();
            log.info("为 appId: {} 加载 {} 条历史记录到聊天记忆", appId, historyList.size());

            // 先清理历史缓存，防止重复加载（确保记忆中只有本次加载的历史）
            chatMemory.clear();

            // 加载历史记录到记忆中（按时间顺序添加）
            // UserMessage.from() 方法用于创建用户消息
            // AiMessage.from() 方法用于创建AI消息
            // chatMemory.add() 方法用于顺序添加历史消息到 chatMemory 中
            int loadedCount = 0;
            for (ChatHistory history : historyList) {
                // 空值保护
                if (history == null || StrUtil.isBlank(history.getMessage())) {
                    log.warn("跳过无效的历史记录: appId={}, historyId={}", appId,
                            history != null ? history.getId() : "null");
                    continue;
                }

                // 根据消息类型区分处理
                String messageType = history.getMessageType();
                if (ChatHistoryTypeEnum.USER.getValue().equals(messageType)) {
                    // 加载用户消息到记忆
                    chatMemory.add(UserMessage.from(history.getMessage()));
                    loadedCount++;
                } else if (ChatHistoryTypeEnum.AI.getValue().equals(messageType)) {
                    // 加载 AI 消息到记忆
                    chatMemory.add(AiMessage.from(history.getMessage()));
                    loadedCount++;
                } else {
                    log.warn("跳过未知类型的历史记录: appId={}, messageType={}", appId, messageType);
                }
            }

            log.info("加载对话历史到记忆成功: appId={}, loadedCount={}", appId, loadedCount);
            return true;
        } catch (Exception e) {
            log.error("【AI 服务实例缓存】加载对话历史失败，appId: {}，错误: {}", appId, e.getMessage(), e);
            return false;
        }
    }
}
