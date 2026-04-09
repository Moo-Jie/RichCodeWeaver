package com.rich.app.controller;

import com.rich.app.service.RagParamService;
import com.rich.common.constant.UserConstant;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.common.exception.ThrowUtils;
import com.rich.common.model.BaseResponse;
import com.rich.common.utils.ResultUtils;
import com.rich.model.annotation.AuthCheck;
import com.rich.model.dto.ragparam.RagParamUpdateRequest;
import com.rich.model.entity.RagParam;
import com.rich.model.vo.RagParamVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RAG 参数配置管理接口
 * 提供 RAG 管道各阶段参数的查询与更新功能（仅管理员）
 *
 * @author DuRuiChi
 * @create 2026/3/27
 */
@Slf4j
@RestController
@RequestMapping("/generator/rag/param")
public class RagParamController {

    @Resource
    private RagParamService ragParamService;

    /**
     * 获取所有 RAG 参数（管理员）
     */
    @GetMapping("/list")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<RagParamVO>> listRagParams() {
        return ResultUtils.success(ragParamService.listAllParams());
    }

    /**
     * 更新 RAG 参数值（管理员）
     * 只允许修改 paramValue，paramKey/paramType/paramGroup 由系统固定
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateRagParam(@RequestBody RagParamUpdateRequest updateRequest) {
        ThrowUtils.throwIf(updateRequest == null || updateRequest.getId() == null,
                ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(updateRequest.getParamValue() == null,
                ErrorCode.PARAMS_ERROR, "参数值不能为 null");

        RagParam existing = ragParamService.getById(updateRequest.getId());
        ThrowUtils.throwIf(existing == null, ErrorCode.NOT_FOUND_ERROR, "参数不存在");

        // 按类型校验格式
        String paramType = existing.getParamType();
        String newValue = updateRequest.getParamValue().trim();
        validateParamValue(paramType, newValue, existing.getParamKey());

        RagParam ragParam = new RagParam();
        ragParam.setId(updateRequest.getId());
        ragParam.setParamValue(newValue);
        boolean result = ragParamService.updateById(ragParam);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        log.info("【RAG参数】参数 [{}] 已更新为: {}", existing.getParamKey(), newValue);
        return ResultUtils.success(true);
    }

    /**
     * 校验参数值格式是否符合参数类型
     */
    private void validateParamValue(String paramType, String value, String paramKey) {
        if (value.isBlank() && !"textarea".equals(paramType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数值不能为空");
        }
        switch (paramType) {
            case "int" -> {
                try {
                    Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR,
                            "参数 [" + paramKey + "] 类型为整数，值 '" + value + "' 格式不正确");
                }
            }
            case "double" -> {
                try {
                    double d = Double.parseDouble(value);
                    if (paramKey.equals("min_score") && (d < 0 || d > 1)) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR,
                                "min_score 范围为 0~1，当前值: " + value);
                    }
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR,
                            "参数 [" + paramKey + "] 类型为小数，值 '" + value + "' 格式不正确");
                }
            }
            case "textarea" -> {
                // 文本类型无需格式校验，但注入模板必须包含占位符
                if (paramKey.equals("injection_prompt_template")) {
                    if (!value.contains("{{userMessage}}") || !value.contains("{{contents}}")) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR,
                                "注入模板必须包含 {{userMessage}} 和 {{contents}} 占位符");
                    }
                }
            }
            default -> { /* 其他类型不做校验 */ }
        }
    }
}
