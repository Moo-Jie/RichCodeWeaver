package com.rich.social.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.rich.model.entity.AppHotStat;
import com.rich.model.vo.AppHotStatVO;

/**
 * 产物热点统计 服务层
 * 提供热点数据的查询与增减操作
 *
 * @author DuRuiChi
 * @since 2026-03-25
 */
public interface AppHotStatService extends IService<AppHotStat> {

    /**
     * 获取产物热点统计VO（含当前用户的点赞/收藏状态）
     *
     * @param appId  产物id
     * @param userId 当前用户id（可为null）
     * @return 热点统计VO
     */
    AppHotStatVO getHotStatVO(Long appId, Long userId);

    /**
     * 确保热点统计记录存在（不存在则初始化）
     *
     * @param appId 产物id
     */
    void ensureStatExists(Long appId);

    /**
     * 递增指定计数字段
     *
     * @param appId     产物id
     * @param fieldName 字段名（likeCount/shareCount/favoriteCount/commentCount）
     */
    void incrementField(Long appId, String fieldName);

    /**
     * 递减指定计数字段（不会低于0）
     *
     * @param appId     产物id
     * @param fieldName 字段名（likeCount/shareCount/favoriteCount/commentCount）
     */
    void decrementField(Long appId, String fieldName);

    /**
     * 分页查询热门产物（按综合热度得分降序）
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 分页热点统计数据
     */
    Page<AppHotStat> listHotApps(long pageNum, long pageSize);
}
