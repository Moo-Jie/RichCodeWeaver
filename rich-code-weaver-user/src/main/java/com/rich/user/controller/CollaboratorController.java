package com.rich.user.controller;

import cn.hutool.json.JSONUtil;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.common.model.BaseResponse;
import com.rich.common.utils.ResultUtils;
import com.rich.model.dto.collaborator.CollaboratorHandleRequest;
import com.rich.model.dto.collaborator.CollaboratorInviteRequest;
import com.rich.model.entity.User;
import com.rich.model.vo.AppCollaboratorVO;
import com.rich.model.vo.ChatMessageVO;
import com.rich.user.mapper.AppCollaboratorMapper;
import com.rich.user.service.AppCollaboratorService;
import com.rich.user.service.ChatMessageService;
import com.rich.user.service.UserService;
import com.rich.user.websocket.ChatWebSocketHandler;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 产物协作者控制器
 * 提供协作者邀请、处理、查询、移除等接口
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@RestController
@RequestMapping("/collaborator")
public class CollaboratorController {

    @Resource
    private AppCollaboratorService appCollaboratorService;

    @Resource
    private UserService userService;

    @Resource
    private ChatWebSocketHandler chatWebSocketHandler;

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private AppCollaboratorMapper collaboratorMapper;

    /**
     * 邀请好友成为产物协作者
     * 前置校验：当前用户身份不能是个人（individual）
     *
     * @param inviteRequest 邀请请求
     * @param request       HTTP请求对象
     * @return 协作关系id
     */
    @PostMapping("/invite")
    public BaseResponse<Long> inviteCollaborator(@RequestBody CollaboratorInviteRequest inviteRequest,
                                                  HttpServletRequest request) {
        ThrowUtils.throwIf(inviteRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(inviteRequest.getAppId() == null || inviteRequest.getAppId() <= 0,
                ErrorCode.PARAMS_ERROR, "产物id无效");
        ThrowUtils.throwIf(inviteRequest.getUserId() == null || inviteRequest.getUserId() <= 0,
                ErrorCode.PARAMS_ERROR, "被邀请用户id无效");

        User loginUser = userService.getLoginUser(request);

        // 校验身份：个人用户不能邀请协作者
        if ("individual".equals(loginUser.getUserIdentity())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "个人用户无法邀请协作者，请先升级身份");
        }

        Long collabId = appCollaboratorService.inviteCollaborator(
                inviteRequest.getAppId(),
                inviteRequest.getUserId(),
                loginUser.getId(),
                inviteRequest.getRole()
        );

        // 查询产物名称用于聊天消息展示
        String appName = getAppName(inviteRequest.getAppId());
        String appCover = getAppCover(inviteRequest.getAppId());
        String role = inviteRequest.getRole() != null ? inviteRequest.getRole() : "editor";

        // 将协作邀请作为聊天消息落库（messageType = collab_invite）
        cn.hutool.json.JSONObject contentJson = new cn.hutool.json.JSONObject();
        contentJson.set("collabId", collabId);
        contentJson.set("appId", inviteRequest.getAppId());
        contentJson.set("appName", appName);
        contentJson.set("appCover", appCover);
        contentJson.set("role", role);
        ChatMessageVO messageVO = chatMessageService.sendMessage(
                loginUser.getId(), inviteRequest.getUserId(),
                contentJson.toString(), "collab_invite");

        // 通过WebSocket实时推送聊天消息
        chatWebSocketHandler.pushChatMessage(
                loginUser.getId(), inviteRequest.getUserId(), messageVO);

        return ResultUtils.success(collabId);
    }

    /**
     * 处理协作邀请（接受/拒绝）
     *
     * @param handleRequest 处理请求
     * @param request       HTTP请求对象
     * @return 操作是否成功
     */
    @PostMapping("/handle")
    public BaseResponse<Boolean> handleInvitation(@RequestBody CollaboratorHandleRequest handleRequest,
                                                   HttpServletRequest request) {
        ThrowUtils.throwIf(handleRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(handleRequest.getId() == null || handleRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR, "协作关系id无效");
        ThrowUtils.throwIf(handleRequest.getAction() == null ||
                        (handleRequest.getAction() != 1 && handleRequest.getAction() != 2),
                ErrorCode.PARAMS_ERROR, "无效的处理动作");

        User loginUser = userService.getLoginUser(request);
        boolean result = appCollaboratorService.handleInvitation(
                handleRequest.getId(), loginUser.getId(), handleRequest.getAction());

        // 处理成功后发送结果消息给邀请人
        if (result) {
            var collab = appCollaboratorService.getById(handleRequest.getId());
            if (collab != null) {
                String appName = getAppName(collab.getAppId());
                String resultText = handleRequest.getAction() == 1
                        ? loginUser.getUserName() + " 已接受「" + appName + "」的协作邀请"
                        : loginUser.getUserName() + " 已拒绝「" + appName + "」的协作邀请";
                ChatMessageVO msgVO = chatMessageService.sendMessage(
                        loginUser.getId(), collab.getInviterId(), resultText, "text");
                chatWebSocketHandler.pushChatMessage(
                        loginUser.getId(), collab.getInviterId(), msgVO);
            }
        }

        return ResultUtils.success(result);
    }

    /**
     * 移除协作者（仅产物所有者可操作）
     *
     * @param appId   产物id
     * @param userId  被移除的协作者用户id
     * @param request HTTP请求对象
     * @return 操作是否成功
     */
    @PostMapping("/remove")
    public BaseResponse<Boolean> removeCollaborator(@RequestParam Long appId,
                                                     @RequestParam Long userId,
                                                     HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "产物id无效");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户id无效");
        User loginUser = userService.getLoginUser(request);
        boolean result = appCollaboratorService.removeCollaborator(appId, userId, loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 获取指定产物的协作者列表
     *
     * @param appId 产物id
     * @return 协作者列表VO
     */
    @GetMapping("/list")
    public BaseResponse<List<AppCollaboratorVO>> listCollaborators(@RequestParam Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "产物id无效");
        List<AppCollaboratorVO> collaborators = appCollaboratorService.listCollaborators(appId);
        return ResultUtils.success(collaborators);
    }

    /**
     * 获取当前用户收到的待处理协作邀请列表
     *
     * @param request HTTP请求对象
     * @return 待处理的协作邀请列表
     */
    @GetMapping("/pending")
    public BaseResponse<List<AppCollaboratorVO>> listPendingInvitations(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<AppCollaboratorVO> pending = appCollaboratorService.listPendingInvitations(loginUser.getId());
        return ResultUtils.success(pending);
    }

    /**
     * 检查当前用户是否为指定产物的协作者
     *
     * @param appId   产物id
     * @param request HTTP请求对象
     * @return 是否为协作者
     */
    @GetMapping("/check")
    public BaseResponse<Boolean> checkCollaborator(@RequestParam Long appId, HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "产物id无效");
        User loginUser = userService.getLoginUser(request);
        boolean isCollab = appCollaboratorService.isCollaborator(appId, loginUser.getId());
        return ResultUtils.success(isCollab);
    }

    // ===== 内部辅助方法 =====

    private String getAppName(Long appId) {
        List<Map<String, Object>> infos = collaboratorMapper.selectAppInfoByIds(List.of(appId));
        if (!infos.isEmpty()) {
            Object name = infos.get(0).get("appName");
            return name != null ? name.toString() : "未知产物";
        }
        return "未知产物";
    }

    private String getAppCover(Long appId) {
        List<Map<String, Object>> infos = collaboratorMapper.selectAppInfoByIds(List.of(appId));
        if (!infos.isEmpty()) {
            Object cover = infos.get(0).get("cover");
            return cover != null ? cover.toString() : null;
        }
        return null;
    }
}
