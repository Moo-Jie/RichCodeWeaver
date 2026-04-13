package com.rich.codeweaver.controller.user;

import com.rich.codeweaver.common.model.BaseResponse;
import com.rich.codeweaver.common.utils.ResultUtils;
import com.rich.codeweaver.common.exception.ErrorCode;
import com.rich.codeweaver.common.exception.ThrowUtils;
import com.rich.codeweaver.model.dto.friendship.FriendAddRequest;
import com.rich.codeweaver.model.dto.friendship.FriendHandleRequest;
import com.rich.codeweaver.model.entity.User;
import com.rich.codeweaver.model.vo.UserFriendshipVO;
import com.rich.codeweaver.model.vo.UserVO;
import com.rich.codeweaver.service.user.UserFriendshipService;
import com.rich.codeweaver.service.user.UserService;
import com.rich.codeweaver.websocket.ChatWebSocketHandler;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 好友关系控制器
 * 提供好友申请、处理、查询、删除等接口
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@RestController
@RequestMapping("/user/friend")
public class FriendController {

    @Resource
    private UserFriendshipService userFriendshipService;

    @Resource
    private UserService userService;

    @Resource
    private ChatWebSocketHandler chatWebSocketHandler;

    /**
     * 发送好友申请
     *
     * @param friendAddRequest 好友申请请求
     * @param request          HTTP请求对象
     * @return 好友关系id
     */
    @PostMapping("/add")
    public BaseResponse<Long> sendFriendRequest(@RequestBody FriendAddRequest friendAddRequest,
                                                HttpServletRequest request) {
        ThrowUtils.throwIf(friendAddRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(friendAddRequest.getFriendId() == null || friendAddRequest.getFriendId() <= 0,
                ErrorCode.PARAMS_ERROR, "目标用户id无效");
        User loginUser = userService.getLoginUser(request);
        Long friendshipId = userFriendshipService.sendFriendRequest(
                loginUser.getId(), friendAddRequest.getFriendId(), friendAddRequest.getRemark());

        // 通过WebSocket推送好友申请通知给对方
        chatWebSocketHandler.pushFriendNotification(friendAddRequest.getFriendId(),
                new FriendNotificationData("newRequest", loginUser.getUserName(), loginUser.getUserAvatar()));

        return ResultUtils.success(friendshipId);
    }

    /**
     * 处理好友申请（同意/拒绝）
     *
     * @param friendHandleRequest 处理请求
     * @param request             HTTP请求对象
     * @return 操作是否成功
     */
    @PostMapping("/handle")
    public BaseResponse<Boolean> handleFriendRequest(@RequestBody FriendHandleRequest friendHandleRequest,
                                                     HttpServletRequest request) {
        ThrowUtils.throwIf(friendHandleRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(friendHandleRequest.getId() == null || friendHandleRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR, "好友关系id无效");
        User loginUser = userService.getLoginUser(request);
        boolean result = userFriendshipService.handleFriendRequest(
                friendHandleRequest.getId(), loginUser.getId(), friendHandleRequest.getAction());
        return ResultUtils.success(result);
    }

    /**
     * 删除好友
     *
     * @param friendId 好友用户id
     * @param request  HTTP请求对象
     * @return 操作是否成功
     */
    @PostMapping("/remove")
    public BaseResponse<Boolean> removeFriend(@RequestParam Long friendId, HttpServletRequest request) {
        ThrowUtils.throwIf(friendId == null || friendId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean result = userFriendshipService.removeFriend(loginUser.getId(), friendId);
        return ResultUtils.success(result);
    }

    /**
     * 获取好友列表
     *
     * @param request HTTP请求对象
     * @return 好友列表VO
     */
    @GetMapping("/list")
    public BaseResponse<List<UserFriendshipVO>> listFriends(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<UserFriendshipVO> friends = userFriendshipService.listFriends(loginUser.getId());
        return ResultUtils.success(friends);
    }

    /**
     * 获取收到的待处理好友申请列表
     *
     * @param request HTTP请求对象
     * @return 待处理的好友申请列表
     */
    @GetMapping("/pending")
    public BaseResponse<List<UserFriendshipVO>> listPendingRequests(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<UserFriendshipVO> pending = userFriendshipService.listPendingRequests(loginUser.getId());
        return ResultUtils.success(pending);
    }

    /**
     * 搜索用户（用于添加好友时查找用户）
     *
     * @param keyword 搜索关键词（用户名模糊匹配）
     * @param request HTTP请求对象
     * @return 匹配的用户列表
     */
    @GetMapping("/search")
    public BaseResponse<List<UserVO>> searchUsers(@RequestParam String keyword, HttpServletRequest request) {
        ThrowUtils.throwIf(keyword == null || keyword.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "搜索关键词不能为空");
        User loginUser = userService.getLoginUser(request);
        // 通过UserService搜索用户（排除自己）
        List<UserVO> users = userService.searchUsersByName(keyword.trim(), loginUser.getId());
        return ResultUtils.success(users);
    }

    /**
     * 好友申请通知数据（内部用，推送WebSocket使用）
     */
    private record FriendNotificationData(String action, String fromUserName, String fromUserAvatar) {
    }
}
