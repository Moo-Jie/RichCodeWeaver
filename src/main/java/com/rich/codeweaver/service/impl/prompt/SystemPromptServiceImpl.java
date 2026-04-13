package com.rich.codeweaver.service.impl.prompt;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.codeweaver.mapper.SystemPromptMapper;
import com.rich.codeweaver.service.prompt.SystemPromptService;
import com.rich.codeweaver.model.dto.systemPrompt.SystemPromptQueryRequest;
import com.rich.codeweaver.model.entity.SystemPrompt;
import com.rich.codeweaver.model.vo.SystemPromptVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统提示词 服务层实现
 *
 * @author DuRuiChi
 */
@Slf4j
@Service
public class SystemPromptServiceImpl extends ServiceImpl<SystemPromptMapper, SystemPrompt>
        implements SystemPromptService {

    @Override
    public SystemPromptVO getSystemPromptVO(SystemPrompt systemPrompt) {
        if (systemPrompt == null) {
            return null;
        }
        SystemPromptVO vo = new SystemPromptVO();
        vo.setId(systemPrompt.getId());
        vo.setPromptName(systemPrompt.getPromptName());
        vo.setPromptKey(systemPrompt.getPromptKey());
        vo.setPromptContent(systemPrompt.getPromptContent());
        vo.setDescription(systemPrompt.getDescription());
        vo.setUserId(systemPrompt.getUserId());
        vo.setCreateTime(systemPrompt.getCreateTime());
        vo.setUpdateTime(systemPrompt.getUpdateTime());
        return vo;
    }

    @Override
    public List<SystemPromptVO> getSystemPromptVOList(List<SystemPrompt> systemPromptList) {
        if (systemPromptList == null) {
            return List.of();
        }
        return systemPromptList.stream().map(this::getSystemPromptVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(SystemPromptQueryRequest queryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (queryRequest == null) {
            return queryWrapper;
        }
        Long id = queryRequest.getId();
        String promptName = queryRequest.getPromptName();
        String promptKey = queryRequest.getPromptKey();

        if (id != null && id > 0) {
            queryWrapper.eq("id", id);
        }
        if (StrUtil.isNotBlank(promptName)) {
            queryWrapper.like("promptName", promptName);
        }
        if (StrUtil.isNotBlank(promptKey)) {
            queryWrapper.like("promptKey", promptKey);
        }

        // 默认按 id 升序
        queryWrapper.orderBy("id", true);
        return queryWrapper;
    }

    @Override
    public String getPromptContentByKey(String promptKey) {
        if (StrUtil.isBlank(promptKey)) {
            log.warn("promptKey 为空，无法查询提示词内容");
            return null;
        }
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("promptKey", promptKey)
                .eq("isDelete", 0);
        SystemPrompt systemPrompt = this.getOne(queryWrapper);
        if (systemPrompt == null) {
            log.warn("未找到 promptKey={} 对应的系统提示词", promptKey);
            return null;
        }
        return systemPrompt.getPromptContent();
    }
}
