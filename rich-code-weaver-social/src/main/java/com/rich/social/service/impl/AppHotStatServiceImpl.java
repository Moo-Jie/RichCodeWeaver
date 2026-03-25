package com.rich.social.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.model.entity.AppHotStat;
import com.rich.model.vo.AppHotStatVO;
import com.rich.social.mapper.AppHotStatMapper;
import com.rich.social.service.AppHotStatService;
import com.rich.social.service.AppLikeService;
import com.rich.social.service.AppFavoriteService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * 产物热点统计 服务实现
 * 实现热点数据的查询与原子增减操作
 *
 * @author DuRuiChi
 * @since 2026-03-25
 */
@Service
public class AppHotStatServiceImpl extends ServiceImpl<AppHotStatMapper, AppHotStat>
        implements AppHotStatService {

    @Resource
    @Lazy
    private AppLikeService appLikeService;

    @Resource
    @Lazy
    private AppFavoriteService appFavoriteService;

    @Override
    public AppHotStatVO getHotStatVO(Long appId, Long userId) {
        ensureStatExists(appId);
        QueryWrapper query = QueryWrapper.create()
                .from(AppHotStat.class)
                .where("appId = ?", appId);
        AppHotStat stat = getOne(query);

        AppHotStatVO vo = new AppHotStatVO();
        vo.setAppId(appId);
        vo.setLikeCount(stat != null ? stat.getLikeCount() : 0);
        vo.setShareCount(stat != null ? stat.getShareCount() : 0);
        vo.setFavoriteCount(stat != null ? stat.getFavoriteCount() : 0);
        vo.setCommentCount(stat != null ? stat.getCommentCount() : 0);

        // 填充当前用户的交互状态
        if (userId != null) {
            vo.setHasLiked(appLikeService.hasLiked(appId, userId));
            vo.setHasFavorited(appFavoriteService.hasFavorited(appId, userId));
        } else {
            vo.setHasLiked(false);
            vo.setHasFavorited(false);
        }
        return vo;
    }

    @Override
    public void ensureStatExists(Long appId) {
        QueryWrapper query = QueryWrapper.create()
                .from(AppHotStat.class)
                .where("appId = ?", appId);
        if (count(query) == 0) {
            AppHotStat stat = AppHotStat.builder()
                    .appId(appId)
                    .likeCount(0)
                    .shareCount(0)
                    .favoriteCount(0)
                    .commentCount(0)
                    .build();
            save(stat);
        }
    }

    @Override
    public void incrementField(Long appId, String fieldName) {
        ensureStatExists(appId);
        // 使用 UpdateChain 进行原子递增
        UpdateChain.of(AppHotStat.class)
                .setRaw(fieldName, fieldName + " + 1")
                .where("appId = ?", appId)
                .update();
    }

    @Override
    public void decrementField(Long appId, String fieldName) {
        ensureStatExists(appId);
        // 使用 UpdateChain 进行原子递减，不低于0
        UpdateChain.of(AppHotStat.class)
                .setRaw(fieldName, "GREATEST(" + fieldName + " - 1, 0)")
                .where("appId = ?", appId)
                .update();
    }
}
