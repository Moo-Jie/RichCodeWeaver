package com.rich.prompt.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.model.dto.systemPrompt.SystemPromptQueryRequest;
import com.rich.model.entity.SystemPrompt;
import com.rich.model.vo.SystemPromptVO;
import com.rich.prompt.mapper.SystemPromptMapper;
import com.rich.prompt.service.SystemPromptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    /**
     * aiPrompt 目录的绝对路径
     * 开发环境：${user.dir}/aiPrompt
     * 生产环境：通过启动参数 -Dprompt.base-path=/path/to/aiPrompt 指定
     */
    @Value("${prompt.base-path}")
    private String promptBasePath;

    @Override
    public SystemPromptVO getSystemPromptVO(SystemPrompt systemPrompt) {
        if (systemPrompt == null) {
            return null;
        }
        SystemPromptVO vo = new SystemPromptVO();
        vo.setId(systemPrompt.getId());
        vo.setPromptName(systemPrompt.getPromptName());
        vo.setFilePath(systemPrompt.getFilePath());
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
        String filePath = queryRequest.getFilePath();

        if (id != null && id > 0) {
            queryWrapper.eq("id", id);
        }
        if (StrUtil.isNotBlank(promptName)) {
            queryWrapper.like("promptName", promptName);
        }
        if (StrUtil.isNotBlank(filePath)) {
            queryWrapper.like("filePath", filePath);
        }

        // 默认按 id 升序
        queryWrapper.orderBy("id", true);
        return queryWrapper;
    }

    @Override
    public String readFileContent(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件路径不能为空");
        }
        try {
            Path absolutePath = resolveAbsolutePath(filePath);
            if (!Files.exists(absolutePath)) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, 
                    "提示词文件不存在: " + absolutePath);
            }
            String content = Files.readString(absolutePath, StandardCharsets.UTF_8);
            log.debug("读取提示词文件成功: {}", absolutePath);
            return content;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("读取提示词文件失败: filePath={}", filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取提示词文件失败：" + e.getMessage());
        }
    }

    @Override
    public void writeFileContent(String filePath, String content) {
        if (StrUtil.isBlank(filePath)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件路径不能为空");
        }
        try {
            Path absolutePath = resolveAbsolutePath(filePath);
            // 确保父目录存在
            Files.createDirectories(absolutePath.getParent());
            Files.writeString(absolutePath, content, StandardCharsets.UTF_8);
            log.info("写入提示词文件成功: {}", absolutePath);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("写入提示词文件失败: filePath={}", filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "写入提示词文件失败：" + e.getMessage());
        }
    }

    /**
     * 解析文件的绝对路径
     * 将数据库中存储的相对路径（如 code-generation-strategy-system-prompt.txt）
     * 转换为基于 promptBasePath 的绝对路径
     * 
     * @param filePath 数据库中存储的文件路径（可能是文件名或相对路径）
     * @return 文件的绝对路径
     */
    private Path resolveAbsolutePath(String filePath) {
        // 去除开头的斜杠（如果有）
        String cleanPath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
        // 如果路径包含 aiPrompt/ 前缀，去除它（因为 promptBasePath 已经指向 aiPrompt 目录）
        if (cleanPath.startsWith("aiPrompt/") || cleanPath.startsWith("aiPrompt" + File.separator)) {
            cleanPath = cleanPath.substring("aiPrompt/".length());
        }
        
        // 拼接绝对路径（使用 File.separator 兼容 Windows 和 Linux）
        Path basePath = Paths.get(promptBasePath);
        Path absolutePath = basePath.resolve(cleanPath);
        
        log.debug("解析文件路径: {} -> {}", filePath, absolutePath);
        return absolutePath;
    }
}
