package com.rich.user.controller;

import com.rich.common.model.BaseResponse;
import com.rich.common.utils.ResultUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查接口
 */
@RestController("userHealthController")
@RequestMapping("/user/health")
public class HealthController {

    @GetMapping("/check")
    public BaseResponse<Map<String, Object>> healthCheck() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("module", "user");
        result.put("timestamp", System.currentTimeMillis());
        return ResultUtils.success(result);
    }
}
