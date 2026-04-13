package com.rich.codeweaver.service.social;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.rich.codeweaver.model.entity.AppFavorite;

/**
 * 产物收藏 服务层
 * 提供产物收藏/取消收藏及我的收藏列表查询
 *
 * @author DuRuiChi
 * @since 2026-03-25
 */
public interface AppFavoriteService extends IService<AppFavorite> {

    /**
     * 切换收藏状态（已收藏则取消，未收藏则新增）
     *
     * @param appId  产物id
     * @param userId 用户id
     * @return true=收藏成功, false=取消收藏成功
     */
    boolean toggleFavorite(Long appId, Long userId);

    /**
     * 检查用户是否已收藏
     *
     * @param appId  产物id
     * @param userId 用户id
     * @return 是否已收藏
     */
    boolean hasFavorited(Long appId, Long userId);

    /**
     * 分页查询用户的收藏列表（返回收藏记录，前端根据appId单独查热点数据）
     *
     * @param userId   用户id
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 收藏记录分页
     */
    Page<AppFavorite> listMyFavorites(Long userId, long pageNum, long pageSize);
}
