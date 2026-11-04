package com.rich.app.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.app.mapper.AiUserProfileMapper;
import com.rich.app.service.AiUserProfileService;
import com.rich.model.entity.AiUserProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI 结构化用户画像 服务层实现
 * 核心逻辑：
 * 1. 按 userId 唯一查询画像记录
 * 2. 创建或更新时仅覆盖非空字段，避免 Agent 单次调用覆盖所有历史数据
 *
 * @author DuRuiChi
 * @create 2026/5/20
 */
@Slf4j
@Service
public class AiUserProfileServiceImpl extends ServiceImpl<AiUserProfileMapper, AiUserProfile> implements AiUserProfileService {

    @Override
    public AiUserProfile getByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            return null;
        }
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(AiUserProfile::getUserId, userId)
                .limit(1);
        return this.getOne(queryWrapper);
    }

    @Override
    public AiUserProfile createOrUpdate(Long userId, AiUserProfile profile) {
        if (userId == null || userId <= 0) {
            log.warn("【用户画像】userId 无效，跳过更新");
            return null;
        }

        AiUserProfile existing = getByUserId(userId);

        if (existing == null) {
            // 首次创建
            profile.setUserId(userId);
            this.save(profile);
            log.info("【用户画像】首次创建: userId={}", userId);
            return getByUserId(userId);
        }

        // 增量更新：仅覆盖非空字段
        mergeNonNullFields(existing, profile);
        this.updateById(existing);
        log.info("【用户画像】增量更新: userId={}", userId);
        return existing;
    }

    /**
     * 将 source 中非空字段合并到 target
     * 避免 Agent 单次调用时覆盖其他历史字段
     */
    private void mergeNonNullFields(AiUserProfile target, AiUserProfile source) {
        if (StrUtil.isNotBlank(source.getPreferredCodeGenType())) {
            target.setPreferredCodeGenType(source.getPreferredCodeGenType());
        }
        if (StrUtil.isNotBlank(source.getPreferredGenMode())) {
            target.setPreferredGenMode(source.getPreferredGenMode());
        }
        if (StrUtil.isNotBlank(source.getTechPreferences())) {
            target.setTechPreferences(source.getTechPreferences());
        }
        if (StrUtil.isNotBlank(source.getStylePreferences())) {
            target.setStylePreferences(source.getStylePreferences());
        }
        if (StrUtil.isNotBlank(source.getDislikedPatterns())) {
            target.setDislikedPatterns(source.getDislikedPatterns());
        }
        if (StrUtil.isNotBlank(source.getUsageGoal())) {
            target.setUsageGoal(source.getUsageGoal());
        }
        if (StrUtil.isNotBlank(source.getExperienceLevel())) {
            target.setExperienceLevel(source.getExperienceLevel());
        }
        if (StrUtil.isNotBlank(source.getPreferredLanguage())) {
            target.setPreferredLanguage(source.getPreferredLanguage());
        }
        if (StrUtil.isNotBlank(source.getCustomNotes())) {
            target.setCustomNotes(source.getCustomNotes());
        }
    }
}
