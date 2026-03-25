package com.rich.social.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.model.entity.AppFavorite;
import com.rich.social.mapper.AppFavoriteMapper;
import com.rich.social.service.AppFavoriteService;
import com.rich.social.service.AppHotStatService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 产物收藏 服务实现
 * 实现收藏/取消收藏的业务逻辑，同步更新热点统计
 *
 * @author DuRuiChi
 * @since 2026-03-25
 */
@Service
public class AppFavoriteServiceImpl extends ServiceImpl<AppFavoriteMapper, AppFavorite>
        implements AppFavoriteService {

    @Resource
    private AppHotStatService appHotStatService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleFavorite(Long appId, Long userId) {
        QueryWrapper query = QueryWrapper.create()
                .from(AppFavorite.class)
                .where("appId = ? AND userId = ?", appId, userId);
        AppFavorite existing = getOne(query);

        if (existing != null) {
            // 取消收藏
            removeById(existing.getId());
            appHotStatService.decrementField(appId, "favoriteCount");
            return false;
        } else {
            // 新增收藏
            AppFavorite favorite = AppFavorite.builder()
                    .appId(appId)
                    .userId(userId)
                    .build();
            save(favorite);
            appHotStatService.incrementField(appId, "favoriteCount");
            return true;
        }
    }

    @Override
    public boolean hasFavorited(Long appId, Long userId) {
        if (userId == null) return false;
        QueryWrapper query = QueryWrapper.create()
                .from(AppFavorite.class)
                .where("appId = ? AND userId = ?", appId, userId);
        return count(query) > 0;
    }

    @Override
    public Page<AppFavorite> listMyFavorites(Long userId, long pageNum, long pageSize) {
        QueryWrapper query = QueryWrapper.create()
                .from(AppFavorite.class)
                .where("userId = ?", userId)
                .orderBy("createTime DESC");
        return page(Page.of(pageNum, pageSize), query);
    }
}
