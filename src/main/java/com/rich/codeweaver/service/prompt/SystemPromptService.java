package com.rich.codeweaver.service.prompt;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.rich.codeweaver.model.dto.systemPrompt.SystemPromptQueryRequest;
import com.rich.codeweaver.model.entity.SystemPrompt;
import com.rich.codeweaver.model.vo.SystemPromptVO;

import java.util.List;

/**
 * 系统提示词 服务层接口
 *
 * @author DuRuiChi
 */
public interface SystemPromptService extends IService<SystemPrompt> {

    /**
     * 实体转视图对象
     *
     * @param systemPrompt 实体
     * @return SystemPromptVO
     */
    SystemPromptVO getSystemPromptVO(SystemPrompt systemPrompt);

    /**
     * 实体列表转视图对象列表
     *
     * @param systemPromptList 实体列表
     * @return List<SystemPromptVO>
     */
    List<SystemPromptVO> getSystemPromptVOList(List<SystemPrompt> systemPromptList);

    /**
     * 构造查询条件
     *
     * @param queryRequest 查询请求
     * @return QueryWrapper
     */
    QueryWrapper getQueryWrapper(SystemPromptQueryRequest queryRequest);

    /**
     * 根据提示词唯一标识获取提示词内容
     *
     * @param promptKey 提示词唯一标识（如 html-system-prompt）
     * @return 提示词内容，若不存在则返回 null
     */
    String getPromptContentByKey(String promptKey);
}
