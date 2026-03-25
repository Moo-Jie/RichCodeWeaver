package com.rich.social.service;

import com.mybatisflex.core.service.IService;
import com.rich.model.entity.AppShare;

/**
 * 产物转发 服务层
 * 提供产物转发的业务逻辑（转发不可撤销）
 *
 * @author DuRuiChi
 * @since 2026-03-25
 */
public interface AppShareService extends IService<AppShare> {

    /**
     * 记录转发行为并递增热点计数
     *
     * @param appId  产物id
     * @param userId 用户id
     */
    void doShare(Long appId, Long userId);
}
