package com.rich.prompt.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.rich.model.dto.systemPrompt.SystemPromptQueryRequest;
import com.rich.model.entity.SystemPrompt;
import com.rich.model.vo.SystemPromptVO;

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
     * 读取提示词文件内容
     *
     * @param filePath 文件路径（相对于 resources）
     * @return 文件内容
     */
    String readFileContent(String filePath);

    /**
     * 写入提示词文件内容
     *
     * @param filePath 文件路径（相对于 resources）
     * @param content  文件内容
     */
    void writeFileContent(String filePath, String content);
}
