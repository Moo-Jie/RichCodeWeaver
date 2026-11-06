package com.rich.user.controller;

import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.common.model.BaseResponse;
import com.rich.common.utils.ResultUtils;
import com.rich.model.dto.friendship.FriendAddRequest;
import com.rich.model.dto.friendship.FriendHandleRequest;
import com.rich.model.entity.User;
import com.rich.model.vo.UserFriendshipVO;
import com.rich.model.vo.UserVO;
import com.rich.user.service.UserFriendshipService;
import com.rich.user.service.UserService;
import com.rich.user.websocket.ChatWebSocketHandler;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 好友关系控制器。
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

    @PostMapping("/add")
    public BaseResponse<Long> sendFriendRequest(@RequestBody FriendAddRequest friendAddRequest,
                                                HttpServletRequest request) {
        ThrowUtils.throwIf(friendAddRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(friendAddRequest.getFriendId() == null || friendAddRequest.getFriendId() <= 0,
                ErrorCode.PARAMS_ERROR, "目标用户id无效");
        String remark = friendAddRequest.getRemark() == null ? null : friendAddRequest.getRemark().trim();
        ThrowUtils.throwIf(remark != null && remark.length() > 200,
                ErrorCode.PARAMS_ERROR, "好友申请备注不能超过 200 个字符");
        User loginUser = userService.getLoginUser(request);
        Long friendshipId = userFriendshipService.sendFriendRequest(
                loginUser.getId(), friendAddRequest.getFriendId(), remark);

        chatWebSocketHandler.pushFriendNotification(friendAddRequest.getFriendId(),
                new FriendNotificationData("newRequest", friendshipId, loginUser.getId(),
                        loginUser.getUserName(), loginUser.getUserAvatar()));
        return ResultUtils.success(friendshipId);
    }

    @PostMapping("/handle")
    public BaseResponse<Boolean> handleFriendRequest(@RequestBody FriendHandleRequest friendHandleRequest,
                                                     HttpServletRequest request) {
        ThrowUtils.throwIf(friendHandleRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(friendHandleRequest.getId() == null || friendHandleRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR, "好友关系id无效");
        ThrowUtils.throwIf(friendHandleRequest.getAction() == null
                        || (friendHandleRequest.getAction() != 1 && friendHandleRequest.getAction() != 2),
                ErrorCode.PARAMS_ERROR, "无效的处理动作");

        User loginUser = userService.getLoginUser(request);
        var friendship = userFriendshipService.getById(friendHandleRequest.getId());
        boolean result = userFriendshipService.handleFriendRequest(
                friendHandleRequest.getId(), loginUser.getId(), friendHandleRequest.getAction());
        if (result && friendship != null) {
            String action = friendHandleRequest.getAction() == 1 ? "accepted" : "rejected";
            chatWebSocketHandler.pushFriendNotification(friendship.getUserId(),
                    new FriendNotificationData(action, friendship.getId(), loginUser.getId(),
                            loginUser.getUserName(), loginUser.getUserAvatar()));
        }
        return ResultUtils.success(result);
    }

    @PostMapping("/remove")
    public BaseResponse<Boolean> removeFriend(@RequestParam Long friendId, HttpServletRequest request) {
        ThrowUtils.throwIf(friendId == null || friendId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userFriendshipService.removeFriend(loginUser.getId(), friendId));
    }

    @GetMapping("/list")
    public BaseResponse<List<UserFriendshipVO>> listFriends(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userFriendshipService.listFriends(loginUser.getId()));
    }

    @GetMapping("/pending")
    public BaseResponse<List<UserFriendshipVO>> listPendingRequests(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userFriendshipService.listPendingRequests(loginUser.getId()));
    }

    @GetMapping("/search")
    public BaseResponse<List<UserVO>> searchUsers(@RequestParam String keyword, HttpServletRequest request) {
        ThrowUtils.throwIf(keyword == null || keyword.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "搜索关键词不能为空");
        ThrowUtils.throwIf(keyword.trim().length() > 50, ErrorCode.PARAMS_ERROR, "搜索关键词不能超过 50 个字符");
        User loginUser = userService.getLoginUser(request);
        List<UserVO> users = userService.searchUsersByName(keyword.trim(), loginUser.getId());
        // 好友搜索只返回公开资料，避免泄露账号、邮箱、手机号与权限字段。
        for (UserVO user : users) {
            user.setUserAccount(null);
            user.setPhone(null);
            user.setEmail(null);
            user.setUserRole(null);
            user.setUserIdentity(null);
        }
        return ResultUtils.success(users);
    }

    /**
     * 好友状态通知数据。
     */
    private record FriendNotificationData(String action, Long friendshipId, Long fromUserId,
                                          String fromUserName, String fromUserAvatar) {
    }
}
