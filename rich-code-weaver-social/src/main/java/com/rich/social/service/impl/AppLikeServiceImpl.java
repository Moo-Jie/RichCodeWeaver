package com.rich.social.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.model.entity.AppLike;
import com.rich.social.mapper.AppLikeMapper;
import com.rich.social.service.AppHotStatService;
import com.rich.social.service.AppLikeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 产物点赞 服务实现
 * 实现点赞/取消点赞的业务逻辑，同步更新热点统计
 *
 * @author DuRuiChi
 * @since 2026-03-25
 */
@Service
public class AppLikeServiceImpl extends ServiceImpl<AppLikeMapper, AppLike>
        implements AppLikeService {

    @Resource
    private AppHotStatService appHotStatService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleLike(Long appId, Long userId) {
        QueryWrapper query = QueryWrapper.create()
                .from(AppLike.class)
                .where("appId = ? AND userId = ?", appId, userId);
        AppLike existing = getOne(query);

        if (existing != null) {
            // 取消点赞
            removeById(existing.getId());
            appHotStatService.decrementField(appId, "likeCount");
            return false;
        } else {
            // 新增点赞
            AppLike appLike = AppLike.builder()
                    .appId(appId)
                    .userId(userId)
                    .build();
            save(appLike);
            appHotStatService.incrementField(appId, "likeCount");
            return true;
        }
    }

    @Override
    public boolean hasLiked(Long appId, Long userId) {
        if (userId == null) return false;
        QueryWrapper query = QueryWrapper.create()
                .from(AppLike.class)
                .where("appId = ? AND userId = ?", appId, userId);
        return count(query) > 0;
    }
}
