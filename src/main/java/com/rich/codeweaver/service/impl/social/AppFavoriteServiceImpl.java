package com.rich.codeweaver.service.impl.social;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.codeweaver.common.exception.BusinessException;
import com.rich.codeweaver.common.exception.ErrorCode;
import com.rich.codeweaver.mapper.AppFavoriteMapper;
import com.rich.codeweaver.model.entity.AppFavorite;
import com.rich.codeweaver.service.social.AppFavoriteService;
import com.rich.codeweaver.service.social.AppHotStatService;
import com.rich.codeweaver.utils.SocialRedisHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 产物收藏 服务实现
 * 实现收藏/取消收藏的业务逻辑，同步更新热点统计
 * 使用 Redis 分布式锁防止同一用户并发操作导致的数据不一致
 *
 * @author DuRuiChi
 * @create 2026-03-25
 */
@Service
public class AppFavoriteServiceImpl extends ServiceImpl<AppFavoriteMapper, AppFavorite>
        implements AppFavoriteService {

    @Resource
    private AppHotStatService appHotStatService;

    @Resource
    private SocialRedisHelper socialRedisHelper;

    /**
     * 切换产物收藏状态（已收藏则取消，未收藏则新增）
     * 使用 Redis 分布式锁保证同一用户对同一产物的并发操作串行化
     * 使用 @Transactional 保证收藏记录和热点统计的原子性更新
     *
     * @param appId  产物id
     * @param userId 用户id
     * @return true=收藏成功, false=取消收藏成功
     * @throws BusinessException 操作频繁时抛出（未获取到分布式锁）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleFavorite(Long appId, Long userId) {
        String lockKey = SocialRedisHelper.favoriteLockKey(appId, userId);
        boolean locked = socialRedisHelper.tryLock(lockKey);
        if (!locked) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作频繁，请稍后重试");
        }
        try {
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
        } finally {
            socialRedisHelper.releaseLock(lockKey);
        }
    }

    /**
     * 检查用户是否已收藏指定产物
     *
     * @param appId  产物id
     * @param userId 用户id
     * @return true=已收藏, false=未收藏
     */
    @Override
    public boolean hasFavorited(Long appId, Long userId) {
        if (userId == null) return false;
        QueryWrapper query = QueryWrapper.create()
                .from(AppFavorite.class)
                .where("appId = ? AND userId = ?", appId, userId);
        return count(query) > 0;
    }

    /**
     * 分页查询用户的收藏列表
     *
     * @param userId   用户id
     * @param pageNum  页码（从1开始）
     * @param pageSize 每页数量
     * @return 收藏记录分页，按创建时间降序排列
     */
    @Override
    public Page<AppFavorite> listMyFavorites(Long userId, long pageNum, long pageSize) {
        QueryWrapper query = QueryWrapper.create()
                .from(AppFavorite.class)
                .where("userId = ?", userId)
                .orderBy("createTime DESC");
        return page(Page.of(pageNum, pageSize), query);
    }
}
