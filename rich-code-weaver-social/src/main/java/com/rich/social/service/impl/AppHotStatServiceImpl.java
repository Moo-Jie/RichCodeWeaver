package com.rich.social.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.model.entity.AppHotStat;
import com.rich.model.vo.AppHotStatVO;
import com.rich.social.mapper.AppHotStatMapper;
import com.rich.social.service.AppFavoriteService;
import com.rich.social.service.AppHotStatService;
import com.rich.social.service.AppLikeService;
import com.rich.social.utils.SocialRedisHelper;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 产物热点统计 服务实现
 * 实现热点数据的查询与原子增减操作，支持并发安全的统计数据更新
 *
 * @author DuRuiChi
 * @create 2026-03-25
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

    @Resource
    private SocialRedisHelper socialRedisHelper;

    /**
     * 获取产物热点统计VO（含当前用户的点赞/收藏状态）
     * 若统计记录不存在则自动创建初始记录
     *
     * @param appId  产物id
     * @param userId 当前用户id（可为null，未登录用户）
     * @return 热点统计VO，包含点赞/转发/收藏/评论数及用户交互状态
     */
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

    /**
     * 确保热点统计记录存在（不存在则初始化）
     * 使用 Redis 分布式锁 + 双重检查 + DuplicateKeyException 捕获三重保障防止并发初始化
     *
     * @param appId 产物id
     */
    @Override
    public void ensureStatExists(Long appId) {
        QueryWrapper query = QueryWrapper.create()
                .from(AppHotStat.class)
                .where("appId = ?", appId);
        if (count(query) == 0) {
            // 使用 Redis 分布式锁 + DuplicateKeyException 双重保障防止并发初始化
            String lockKey = SocialRedisHelper.hotStatInitLockKey(appId);
            boolean locked = socialRedisHelper.tryLock(lockKey);
            if (locked) {
                try {
                    // 再次检查（双重检查）
                    if (count(query) == 0) {
                        AppHotStat stat = AppHotStat.builder()
                                .appId(appId)
                                .likeCount(0)
                                .shareCount(0)
                                .favoriteCount(0)
                                .commentCount(0)
                                .build();
                        try {
                            save(stat);
                        } catch (DuplicateKeyException ignored) {
                            // 并发场景下另一线程已插入，安全忽略
                        }
                    }
                } finally {
                    socialRedisHelper.releaseLock(lockKey);
                }
            }
            // 若未获取到锁，说明其他线程正在初始化，可安全跳过
        }
    }

    /**
     * 原子递增指定计数字段
     * 使用 MyBatis-Flex UpdateChain.setRaw() 生成数据库级原子操作
     *
     * @param appId     产物id
     * @param fieldName 字段名（likeCount/shareCount/favoriteCount/commentCount）
     */
    @Override
    public void incrementField(Long appId, String fieldName) {
        ensureStatExists(appId);
        // 使用 UpdateChain 进行原子递增
        UpdateChain.of(AppHotStat.class)
                .setRaw(fieldName, fieldName + " + 1")
                .where("appId = ?", appId)
                .update();
    }

    /**
     * 原子递减指定计数字段（不会低于0）
     * 使用 MyBatis-Flex UpdateChain.setRaw() 生成数据库级原子操作，通过 GREATEST 函数保证非负
     *
     * @param appId     产物id
     * @param fieldName 字段名（likeCount/shareCount/favoriteCount/commentCount）
     */
    @Override
    public void decrementField(Long appId, String fieldName) {
        ensureStatExists(appId);
        // 使用 UpdateChain 进行原子递减，不低于0
        UpdateChain.of(AppHotStat.class)
                .setRaw(fieldName, "GREATEST(" + fieldName + " - 1, 0)")
                .where("appId = ?", appId)
                .update();
    }

    /**
     * 分页查询热门产物列表（按综合热度得分降序）
     * 综合热度得分 = likeCount + shareCount + favoriteCount + commentCount
     * 使用 QueryColumn 算术运算以通过 MyBatis-Flex SQL 安全检查
     *
     * @param pageNum  页码（从1开始）
     * @param pageSize 每页数量
     * @return 分页热点统计数据，按热度降序、更新时间降序排列
     */
    @Override
    public Page<AppHotStat> listHotApps(long pageNum, long pageSize) {
        QueryColumn likeCount = new QueryColumn("likeCount");
        QueryColumn shareCount = new QueryColumn("shareCount");
        QueryColumn favoriteCount = new QueryColumn("favoriteCount");
        QueryColumn commentCount = new QueryColumn("commentCount");
        QueryColumn updateTime = new QueryColumn("updateTime");

        QueryWrapper query = QueryWrapper.create()
                .from(AppHotStat.class)
                .orderBy(
                        likeCount.add(shareCount).add(favoriteCount).add(commentCount).desc(),
                        updateTime.desc()
                );
        return page(Page.of(pageNum, pageSize), query);
    }
}
