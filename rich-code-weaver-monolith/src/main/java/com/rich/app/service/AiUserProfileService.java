package com.rich.app.service;

import com.mybatisflex.core.service.IService;
import com.rich.model.entity.AiUserProfile;

/**
 * AI 结构化用户画像 服务层
 * 负责用户画像的查询、创建与更新
 *
 * @author DuRuiChi
 * @create 2026/5/20
 */
public interface AiUserProfileService extends IService<AiUserProfile> {

    /**
     * 根据 userId 获取用户画像
     * 如果不存在则返回 null
     *
     * @param userId 用户 ID
     * @return 用户画像（不存在返回 null）
     */
    AiUserProfile getByUserId(Long userId);

    /**
     * 创建或更新用户画像（按 userId 幂等）
     * 如果已存在则更新非空字段，不存在则新建
     *
     * @param userId  用户 ID
     * @param profile 需要更新的画像数据（仅非空字段会被写入）
     * @return 更新后的完整用户画像
     */
    AiUserProfile createOrUpdate(Long userId, AiUserProfile profile);
}
