package com.rich.social.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.model.entity.AppLike;
import com.rich.social.mapper.AppLikeMapper;
import com.rich.social.service.AppHotStatService;
import com.rich.social.service.AppLikeService;
import com.rich.social.utils.SocialRedisHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 产物点赞 服务实现
 * 实现点赞/取消点赞的业务逻辑，同步更新热点统计
 * 使用 Redis 分布式锁防止同一用户并发操作导致的数据不一致
 *
 * @author DuRuiChi
 * @create 2026-03-25
 */
@Service
public class AppLikeServiceImpl extends ServiceImpl<AppLikeMapper, AppLike>
        implements AppLikeService {

    @Resource
    private AppHotStatService appHotStatService;

    @Resource
    private SocialRedisHelper socialRedisHelper;

    /**
     * 切换产物点赞状态（已点赞则取消，未点赞则新增）
     * 使用 Redis 分布式锁保证同一用户对同一产物的并发操作串行化
     * 使用 @Transactional 保证点赞记录和热点统计的原子性更新
     *
     * @param appId  产物id
     * @param userId 用户id
     * @return true=点赞成功, false=取消点赞成功
     * @throws BusinessException 操作频繁时抛出（未获取到分布式锁）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleLike(Long appId, Long userId) {
        String lockKey = SocialRedisHelper.likeLockKey(appId, userId);
        boolean locked = socialRedisHelper.tryLock(lockKey);
        if (!locked) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作频繁，请稍后重试");
        }
        try {
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
        } finally {
            socialRedisHelper.releaseLock(lockKey);
        }
    }

    /**
     * 检查用户是否已点赞指定产物
     *
     * @param appId  产物id
     * @param userId 用户id
     * @return true=已点赞, false=未点赞
     */
    @Override
    public boolean hasLiked(Long appId, Long userId) {
        if (userId == null) return false;
        QueryWrapper query = QueryWrapper.create()
                .from(AppLike.class)
                .where("appId = ? AND userId = ?", appId, userId);
        return count(query) > 0;
    }
}
