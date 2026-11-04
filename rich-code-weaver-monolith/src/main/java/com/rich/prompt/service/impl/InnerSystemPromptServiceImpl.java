package com.rich.prompt.service.impl;

import com.rich.prompt.api.InnerSystemPromptService;
import com.rich.prompt.service.SystemPromptService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 系统提示词内部服务实现
 * 作为本地 Spring Bean，供其他模块获取系统提示词内容
 *
 * @author DuRuiChi
 */
@Service
public class InnerSystemPromptServiceImpl implements InnerSystemPromptService {

    @Resource
    private SystemPromptService systemPromptService;

    @Override
    public String getPromptContentByKey(String promptKey) {
        return systemPromptService.getPromptContentByKey(promptKey);
    }
}
