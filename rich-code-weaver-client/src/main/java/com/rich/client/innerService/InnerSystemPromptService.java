package com.rich.client.innerService;

/**
 * 系统提示词内部服务接口
 * 通过 Dubbo 提供系统提示词的远程调用接口，
 * 供 AI 服务工厂在构建 AI 服务实例时获取系统提示词内容
 *
 * @author DuRuiChi
 */
public interface InnerSystemPromptService {

    /**
     * 根据提示词唯一标识获取提示词内容
     *
     * @param promptKey 提示词唯一标识（如 html-system-prompt）
     * @return 提示词内容，若不存在则返回 null
     */
    String getPromptContentByKey(String promptKey);
}
