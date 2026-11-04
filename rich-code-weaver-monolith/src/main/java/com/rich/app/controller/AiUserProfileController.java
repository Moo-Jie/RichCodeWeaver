package com.rich.app.controller;

import cn.hutool.core.bean.BeanUtil;
import com.rich.app.service.AiUserProfileService;
import com.rich.user.api.InnerUserService;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.common.model.BaseResponse;
import com.rich.common.utils.ResultUtils;
import com.rich.model.dto.user.AiUserProfileUpdateRequest;
import com.rich.model.entity.AiUserProfile;
import com.rich.model.entity.User;
import com.rich.model.vo.AiUserProfileVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/generator/userProfile")
public class AiUserProfileController {

    @Resource
    private AiUserProfileService aiUserProfileService;

    @GetMapping("/my")
    public BaseResponse<AiUserProfileVO> getMyUserProfile(HttpServletRequest request) {
        User loginUser = InnerUserService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        AiUserProfile profile = aiUserProfileService.getByUserId(loginUser.getId());
        return ResultUtils.success(toVO(profile));
    }

    @PostMapping("/update/my")
    public BaseResponse<AiUserProfileVO> updateMyUserProfile(@RequestBody AiUserProfileUpdateRequest updateRequest,
                                                             HttpServletRequest request) {
        ThrowUtils.throwIf(updateRequest == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        User loginUser = InnerUserService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        AiUserProfile profile = new AiUserProfile();
        BeanUtil.copyProperties(updateRequest, profile);
        AiUserProfile savedProfile = aiUserProfileService.createOrUpdate(loginUser.getId(), profile);
        return ResultUtils.success(toVO(savedProfile));
    }

    private AiUserProfileVO toVO(AiUserProfile profile) {
        if (profile == null) {
            return null;
        }
        AiUserProfileVO vo = new AiUserProfileVO();
        BeanUtil.copyProperties(profile, vo);
        return vo;
    }
}
