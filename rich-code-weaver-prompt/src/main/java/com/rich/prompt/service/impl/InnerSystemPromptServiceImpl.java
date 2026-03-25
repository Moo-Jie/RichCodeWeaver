package com.rich.prompt.service.impl;

import com.rich.client.innerService.InnerSystemPromptService;
import com.rich.prompt.service.SystemPromptService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 系统提示词内部服务实现
 * 通过 Dubbo 提供远程调用能力，供其他模块获取系统提示词内容
 *
 * @author DuRuiChi
 */
@DubboService
public class InnerSystemPromptServiceImpl implements InnerSystemPromptService {

    @Resource
    private SystemPromptService systemPromptService;

    @Override
    public String getPromptContentByKey(String promptKey) {
        return systemPromptService.getPromptContentByKey(promptKey);
    }
}
